package com.devprofile.DevProfile.service.userData;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class UserDataService {

    @Autowired
    private MongoTemplate mongoTemplate;
    public String findMaxFieldInList(String id) {
        System.out.println("id = " + id);
        Query query = new Query(Criteria.where("userName").is(id));
        BasicDBObject dbObject = mongoTemplate.findOne(query, BasicDBObject.class, "userData");

        String maxField = "";
        double maxValue = Double.NEGATIVE_INFINITY;

        for (String key : dbObject.keySet()) {
            Object value = dbObject.get(key);
            if (value instanceof Number && ((Number) value).doubleValue() > maxValue) {
                maxValue = ((Number) value).doubleValue();
                maxField = key;
            }
        }
        return maxField;
    }
}
