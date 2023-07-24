package com.devprofile.DevProfile.service.commit;


import com.devprofile.DevProfile.entity.CommitKeywordsEntity;
import com.devprofile.DevProfile.entity.UserDataEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
@Service
public class CommitKeywordsService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public String trimQuotes(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        if (str.startsWith("\"")) {
            str = str.substring(1);
        }

        if (str.endsWith("\"")) {
            str = str.substring(0, str.length() - 1);
        }

        return str;
    }

    public Mono<?> addCommitKeywords(String userName,String oid, String keywords) {
        keywords = trimQuotes(keywords);
        keywords = keywords.replace("\\\"", "\"");
        keywords = keywords.replace("\\n", "\n");

        System.out.println("keywords = " + keywords);
        Update update = new Update();
        update.set("oid", oid);
        Update updateUser = new Update();
        updateUser.set("userName", userName);
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode keywordsJson = mapper.readTree(keywords);
            // 이후 jsonNode 객체를 사용할 수 있습니다.
            JsonNode css = keywordsJson.get("cs");
            if (css != null) {
                for (JsonNode cs : css) {
                    update.addToSet("cs", cs.asText());
                    updateUser.addToSet("keywordSet", cs.asText());
                }
            }
            JsonNode langFrames = keywordsJson.get("langFrame");
            if (langFrames != null) {
                for (JsonNode langFrame : langFrames) {
                    update.addToSet("langFramework", langFrame.asText());
                    updateUser.addToSet("keywordSet", langFrame.asText());
                }
            }
            JsonNode features = keywordsJson.get("feature");
            if (features != null) {
                for (JsonNode feature : features) {
                    update.addToSet("featured", feature.asText());
                }
            }
            JsonNode fields = keywordsJson.get("field");
            if (fields != null) {
                for (JsonNode field : fields) {
                    switch (field.asText()) {
                        case ("Game"):
                            updateUser.inc("game");
                            break;
                        case ("Web Backend"):
                            updateUser.inc("webBackend");
                            break;
                        case ("Web Frontend"):
                            updateUser.inc("webFrontend");
                            break;
                        case ("Database"):
                            updateUser.inc("database");
                            break;
                        case ("Mobile"):
                            updateUser.inc("mobile");
                            break;
                        case ("Document"):
                            updateUser.inc("document");
                            break;
                        case ("System Programming"):
                            updateUser.inc("systemProgramming");
                            break;
                        case ("AI"):
                            updateUser.inc("ai");
                            break;
                        case ("Algorithm"):
                            updateUser.inc("algorithm");
                            break;
                    }
                }
            }
            Query query = new Query(Criteria.where("oid").is(oid));
            Query queryUser = new Query(Criteria.where("userName").is(userName));



            mongoTemplate.upsert(queryUser, updateUser, UserDataEntity.class);
            mongoTemplate.upsert(query, update, CommitKeywordsEntity.class);
            return Mono.empty();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
