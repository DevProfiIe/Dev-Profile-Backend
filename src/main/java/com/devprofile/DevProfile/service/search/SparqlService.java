package com.devprofile.DevProfile.service.search;



import org.apache.jena.query.*;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

@Service
public class SparqlService {

    private static final String SPARQL_ENDPOINT = "http://dbpedia.org/sparql";
    private static final int PAGE_SIZE = 100;
    private Map<String, Boolean> visited = new HashMap<>();



    private static Pair<String, Boolean> cleanTitle(String title) {
        title = title.replace("@en", ""); // "@en" 제거
        title = title.replace(" ", "_"); // 띄어쓰기를 "_"로 변경
        String changeTitle = title.replaceAll("_\\(.*?\\)", "");
        Boolean change = false;
        if (!changeTitle.equals(title)) {
            change = true;
        }

        return Pair.of(changeTitle, change);
    }

    public Queue<String> makeStartData(){
        Queue<String> computerKeywordDeque = new ArrayDeque<>();
        computerKeywordDeque.add("Computer_science");
        computerKeywordDeque.add("Web_frameworks");
        computerKeywordDeque.add("Programming_languages");
        computerKeywordDeque.add("Software_architecture");
        computerKeywordDeque.add("Artificial_intelligence");
        computerKeywordDeque.add("Machine_learning");
        computerKeywordDeque.add("Software_engineering");
        computerKeywordDeque.add("Computer_networking");
        computerKeywordDeque.add("Cryptography");
        computerKeywordDeque.add("Computer_graphics");

        visited.put("Computer_science",true);
        visited.put("Web_frameworks",true);
        visited.put("Programming_languages",true);
        visited.put("Software_architecture",true);
        visited.put("Artificial_intelligence",true);
        visited.put("Machine_learning",true);
        visited.put("Software_engineering",true);
        visited.put("Cryptography",true);
        visited.put("Computer_graphics",true);

        return computerKeywordDeque;
    }

    public List<String> sparqlCategory(){
        Queue<String> computerKeywordDeque = makeStartData();
        List<String> category = new ArrayList<>();
        category.addAll(Arrays.asList("Computer_science", "Web_frameworks", "Programming_languages",
                "Software_architecture", "Artificial_intelligence", "Machine_learning",
                "Software_engineering", "Cryptography", "Computer_graphics"));
        int index = 0;
        while (!computerKeywordDeque.isEmpty() && index <= 50) {
            int offset = 0;
            String subject = computerKeywordDeque.poll();
            System.out.println("subject = " + subject);
            try {
                subject = URLEncoder.encode(subject, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            while(true){
                String sparqlQuery =
                        "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
                                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                                "\n" +
                                "SELECT ?category ?label WHERE {\n" +
                                "  <http://dbpedia.org/resource/Category:" + subject + "> skos:broader ?category.\n" +
                                "  ?category rdfs:label ?label.\n" +
                                "  FILTER(LANG(?label) = \"en\")\n" +
                                "}\n" +
                                "ORDER BY ?category\n" +
                                "OFFSET " + offset + "\n" +
                                "LIMIT " + PAGE_SIZE;

                Query query = QueryFactory.create(sparqlQuery);

                try (QueryExecution qexec = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT, query)) {
                    ResultSet results = qexec.execSelect();

                    if (!results.hasNext()) {
                        break;
                    }
                    while (results.hasNext()) {
                        QuerySolution soln = results.nextSolution();
                        Pair<String, Boolean> textChanger = cleanTitle(soln.get("label").toString());
                        String fixedText = textChanger.getFirst();
                        System.out.println("fixedText = " + fixedText);
                        if(!visited.getOrDefault(fixedText, false)){
                            category.add(fixedText);
                            visited.put(fixedText, true);
                            if(!textChanger.getSecond()){
                                computerKeywordDeque.add(fixedText);
                            }
                            index++;
                        }
                    }
                }
                offset += PAGE_SIZE;
            }
        }
        System.out.println(category.size());
        return category;
    }

    public void sparqlEntity() {
        List<String> categories  = sparqlCategory();
        for(String category : categories){

        }
    }
}
