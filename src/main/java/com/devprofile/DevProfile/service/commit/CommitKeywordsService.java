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
import reactor.core.publisher.Mono;
@Service
public class CommitKeywordsService {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public CommitKeywordsService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    private String trimQuotes(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        if (str.charAt(0) == '\"') {
            str = str.substring(1);
        }

        if (str.charAt(str.length() - 1) == '\"') {
            str = str.substring(0, str.length() - 1);
        }

        return str;
    }

    public Mono<?> addCommitKeywords(String userName,String oid, String keywords) {
        keywords = trimQuotes(keywords);
        keywords = keywords.replace("\\\"", "\"");
        keywords = keywords.replace("\\n", "\n");

        System.out.println("keywords = " + keywords);
        Update update = new Update().set("oid", oid);
        Update updateUser = new Update().set("userName", userName);

        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode keywordsJson = mapper.readTree(keywords);
            processKeywords(update, updateUser, keywordsJson);
            Query query = new Query(Criteria.where("oid").is(oid));
            Query queryUser = new Query(Criteria.where("userName").is(userName));
            mongoTemplate.upsert(queryUser, updateUser, UserDataEntity.class);
            mongoTemplate.upsert(query, update, CommitKeywordsEntity.class);
            return Mono.empty();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void processKeywords(Update update, Update updateUser, JsonNode keywordsJson) {
        addCsKeywords(update, updateUser, keywordsJson.get("cs"));
        addLangFrameKeywords(update, updateUser, keywordsJson.get("langFrame"));
        addFeatureKeywords(update, keywordsJson.get("feature"));
        addFieldKeywords(updateUser, keywordsJson.get("field"));
    }

    private void addCsKeywords(Update update, Update updateUser, JsonNode csNode) {
        if (csNode != null) {
            for (JsonNode cs : csNode) {
                update.addToSet("cs", cs.asText());
                updateUser.addToSet("keywordSet", cs.asText());
            }
        }
    }

    private void addLangFrameKeywords(Update update, Update updateUser, JsonNode langFrameNode) {
        if (langFrameNode != null) {
            for (JsonNode langFrame : langFrameNode) {
                update.addToSet("langFramework", langFrame.asText());
                updateUser.addToSet("keywordSet", langFrame.asText());
            }
        }
    }

    private void addFeatureKeywords(Update update, JsonNode featureNode) {
        if (featureNode != null) {
            for (JsonNode feature : featureNode) {
                update.addToSet("featured", feature.asText());
            }
        }
    }

    private void addFieldKeywords(Update updateUser, JsonNode fieldNode) {
        if (fieldNode != null) {
            for (JsonNode field : fieldNode) {
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
    }
}