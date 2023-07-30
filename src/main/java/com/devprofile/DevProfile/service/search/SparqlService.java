package com.devprofile.DevProfile.service.search;



import org.apache.jena.query.*;
import org.springframework.stereotype.Service;

@Service
public class SparqlService {

    public void sparqlTest(){
        String sparqlEndpoint = "http://dbpedia.org/sparql";
        String sparqlQuery =
                "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                        "PREFIX dbr: <http://dbpedia.org/resource/> " +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "SELECT ?framework ?label " +
                        "WHERE { " +
                        "?framework a dbo:SoftwareFramework . " +
                        "?framework dbo:genre dbr:Application_framework . " +
                        "?framework rdfs:label ?label . " +
                        "FILTER (langMatches(lang(?label), \"EN\")) " +
                        "}";

        Query query = QueryFactory.create(sparqlQuery);
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                System.out.println(soln.get("label").toString());
            }
        }
    }

}
