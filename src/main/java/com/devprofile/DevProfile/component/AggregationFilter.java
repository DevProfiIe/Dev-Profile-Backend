package com.devprofile.DevProfile.component;


import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;



import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;

import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import lombok.AllArgsConstructor;

import org.springframework.data.mongodb.core.query.Criteria;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;

@Component
@AllArgsConstructor
public class AggregationFilter {

    private final MongoTemplate mongoTemplate;


    public Pair<Integer,AggregationResults<Map>> runAggregation(String field, List<String> frameworks, List<String> languages, List<String> keywords, int page, int size){
        // Prepare filters
        List<Criteria> filters = new ArrayList<>();

        if(frameworks!= null && !frameworks.isEmpty()){
            for(int i = 0; i < frameworks.size(); i += 2) {
                String name = frameworks.get(i);
                Integer duration = Integer.parseInt(frameworks.get(i + 1));
                filters.add(Criteria.where( "frameworks." + name).gte(duration * 30));
            }
        }

        if(languages!=null&&!languages.isEmpty()){
            for(int i = 0; i < languages.size(); i += 2) {
                String name = languages.get(i);
                Integer duration = Integer.parseInt(languages.get(i + 1));
                filters.add(Criteria.where("languages." + name).gte(duration * 30));
            }
        }

        if(keywords!=null&&!keywords.isEmpty()) filters.add(Criteria.where("styles").all(keywords));
        if(field!=null&&!field.equals(""))filters.add(Criteria.where("field").is(field));
        Aggregation aggregation;
        Aggregation countAggregation;
        if(!filters.isEmpty()){
        countAggregation = newAggregation(
                match(new Criteria().andOperator(filters.toArray(new Criteria[filters.size()]))),
                count().as("count"));
        }
        else{
            countAggregation = newAggregation(count().as("count"));
        }

        AggregationResults<Map> countResult = mongoTemplate.aggregate(countAggregation, "filter", Map.class);
        int totalCount = (int) countResult.getUniqueMappedResult().get("count");

        // Create final aggregation
        int skip = (page - 1) * size;
        if(!filters.isEmpty()){
            aggregation = newAggregation(
                    match(new Criteria().andOperator(filters.toArray(new Criteria[filters.size()]))),
                    sort(Sort.Direction.ASC, "_id"),
                    skip(skip),
                    limit(size)
            );
        }else{
            aggregation = newAggregation(
                    sort(Sort.Direction.ASC, "_id"),
                    skip(skip),
                    limit(size)
            );
        }

        AggregationResults<Map> result = mongoTemplate.aggregate(aggregation, "filter", Map.class);

        return Pair.of(totalCount, result);
    }

}
