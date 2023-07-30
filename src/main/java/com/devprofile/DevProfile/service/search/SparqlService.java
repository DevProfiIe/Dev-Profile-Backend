package com.devprofile.DevProfile.service.search;



import org.apache.jena.query.*;
import org.springframework.stereotype.Service;

@Service
public class SparqlService {

    public void sparqlTest(){
        String sparqlQueryString =
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
                        "SELECT ?software WHERE {?software rdf:type dbo:Software .} LIMIT 10";
        Query query = QueryFactory.create(sparqlQueryString);

        QueryExecution qe = QueryExecution.service("http://dbpedia.org/sparql").query(query).build();
        ResultSet results = qe.execSelect();

        ResultSetFormatter.out(System.out, results, query);

        qe.close();
    }

}
