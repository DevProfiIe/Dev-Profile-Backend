package com.devprofile.DevProfile.service.search;



import com.devprofile.DevProfile.entity.WordEntity;
import com.devprofile.DevProfile.repository.WordRepository;
import org.apache.jena.query.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class SparqlService {

    private static final String SPARQL_ENDPOINT = "http://dbpedia.org/sparql";
    private static final int PAGE_SIZE = 100;
    private static final String ENCODING = "UTF-8";
    private Map<String, Boolean> visited = new HashMap<>();
    private final WordRepository wordRepository;


    public SparqlService(WordRepository wordRepository) {
        this.wordRepository = wordRepository;
    }

    private static WordEntity cleanTitle(String title) {
        WordEntity word = new WordEntity();
        title = title.replace("@en", ""); // "@en" 제거
        word.setKeyword(title.replaceAll(" \\(.*?\\)", "")); // 괄호부 제거
        word.setQueryWord(title.replace(" ", "_")); // 띄어쓰기를 "_"로 변경

        return word;
    }

    public Queue<String> makeStartData(){
        Queue<String> computerKeywordDeque = new ArrayDeque<>();
        computerKeywordDeque.add("Web_framework");
        computerKeywordDeque.add("Programming_language");
        computerKeywordDeque.add("Computer_networking");
        computerKeywordDeque.add("Software_architecture");
        computerKeywordDeque.add("Artificial_intelligence");
        computerKeywordDeque.add("Computer_science");
        computerKeywordDeque.add("Software_engineering");
        computerKeywordDeque.add("Computer_graphics");

        visited.put("Computer science",true);
        visited.put("Web framework",true);
        visited.put("Programming languages",true);
        visited.put("Software architecture",true);
        visited.put("Artificial intelligence",true);
        visited.put("Software engineering",true);
        visited.put("Computer networking",true);
        visited.put("Computer graphics",true);

        return computerKeywordDeque;
    }
    public List<WordEntity> getRelateEntity(String entity, Integer offset) {
        String queryStr = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "\n" +
                "SELECT ?label \n" +
                "WHERE {\n" +
                "  {\n" +
                "    <http://dbpedia.org/resource/"+ entity +"> ?p ?o .\n" +
                "    ?o rdfs:label ?label .\n" +
                "  } UNION {\n" +
                "    ?s ?p <http://dbpedia.org/resource/"+ entity +"> .\n" +
                "    ?s rdfs:label ?label .\n" +
                "  }\n" +
                "  FILTER (lang(?label) = \"en\")\n" +
                "}\n"+
                "OFFSET " + offset + "\n" +
                "LIMIT " + PAGE_SIZE;

        return executeSparqlQuery(queryStr);
    }

    private List<WordEntity> executeSparqlQuery(String queryString) {
        Query query = QueryFactory.create(queryString);

        List<WordEntity> labels = new ArrayList<>();
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT, query)) {
            ResultSet results = qe.execSelect();
            while (results.hasNext()) {
                QuerySolution qs = results.next();
                WordEntity wordEntity = cleanTitle(qs.get("?label").toString());
                labels.add(wordEntity);
            }
        }

        return labels;
    }

    public void sparqlEntity(){
        Queue<String> computerKeywordDeque = makeStartData();
        int index = 0;

        while (!computerKeywordDeque.isEmpty() && index <= 100000) {
            int offset = 0;
            String subject = computerKeywordDeque.poll();
            System.out.println("subject = " + subject);
            subject =getEncodedString(subject);
            List<WordEntity> saveWords = new ArrayList<>();
            while(true){
                List<WordEntity> wordEntities = getRelateEntity(subject, offset);
                if(wordEntities.isEmpty()){
                    wordRepository.saveAll(saveWords);
                    break;
                }else{
                    for(WordEntity wordEntity : wordEntities){
                        if(!visited.getOrDefault(wordEntity.getKeyword(),false)){
                            computerKeywordDeque.add(wordEntity.getQueryWord());
                            saveWords.add(wordEntity);
                            visited.put(wordEntity.getKeyword(), true);
                            index++;
                        }
                    }
                }
                offset += PAGE_SIZE;
            }
        }
    }

    private String getEncodedString(String input) {
        try {
            return URLEncoder.encode(input, ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
