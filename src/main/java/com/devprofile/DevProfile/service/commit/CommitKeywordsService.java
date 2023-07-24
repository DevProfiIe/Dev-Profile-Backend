package com.devprofile.DevProfile.service.commit;


import com.devprofile.DevProfile.entity.CommitKeywordsEntity;
import com.devprofile.DevProfile.entity.UserDataEntity;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CommitKeywordsService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public Mono<?> addCommitKeywords(String userName,String oid, JsonNode keywords){
        Query query = new Query(Criteria.where("oid").is(oid));
        Query queryUser = new Query(Criteria.where("userName").is(userName));
        Update update = new Update();
        Update updateUser = new Update();
        JsonNode langFrames = keywords.get("framework and language");
        for(JsonNode langFrame : langFrames){
            update.addToSet("langFramework", langFrame);
            updateUser.addToSet("keywordSet",langFrame);
        }
        JsonNode css = keywords.get("cs");
        for(JsonNode cs : css){
            update.addToSet("cs", cs);
            updateUser.addToSet("keywordSet",cs);
        }

        update.addToSet("featured",keywords.get("feature"));
        updateUser.addToSet("keywordSet",keywords.get("feature"));

        mongoTemplate.upsert(queryUser, updateUser, UserDataEntity.class);
        mongoTemplate.upsert(query, update, CommitKeywordsEntity.class);
        return Mono.empty();
    }
}
