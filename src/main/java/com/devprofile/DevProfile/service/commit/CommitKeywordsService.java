package com.devprofile.DevProfile.service.commit;


import com.devprofile.DevProfile.entity.CommitKeywordsEntity;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class CommitKeywordsService {
    @Autowired
    private MongoTemplate mongoTemplate;

    public void addCommitKeywords(String oid, JsonNode keywords){
        Query query = new Query(Criteria.where("oid").is(oid));
        Update update = new Update();
        JsonNode langFrames = keywords.get("framework and language");
        for(JsonNode langFrame : langFrames)
            update.addToSet("langFramework", langFrame);
        JsonNode css = keywords.get("cs");
        for(JsonNode cs : css)
            update.addToSet("css", cs);
        update.addToSet("featured",keywords.get("feature"));

        mongoTemplate.upsert(query, update, CommitKeywordsEntity.class);
    }
}
