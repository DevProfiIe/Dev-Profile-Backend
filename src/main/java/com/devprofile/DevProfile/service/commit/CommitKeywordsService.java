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

import java.util.HashSet;
import java.util.Set;

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

    public Mono<?> addCommitKeywords(String userName, String oid, String keywords) {
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
            update.set("userName", userName);
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

    public Mono<?> processMsgScore(String userName, String contents) {
        contents = trimQuotes(contents);
        contents = contents.replace("\\\"", "\"");
        contents = contents.replace("\\n", "\n");

        System.out.println("contents = " + contents);
        Update updateUser = new Update().set("userName", userName);
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode keywordsJson = mapper.readTree(contents);
            addMsgScore(updateUser, keywordsJson.get("msgScore"));
            Query queryUser = new Query(Criteria.where("userName").is(userName));
            mongoTemplate.upsert(queryUser, updateUser, UserDataEntity.class);
            return Mono.empty();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void processKeywords(Update update, Update updateUser, JsonNode keywordsJson) {
        addCsKeywords(update, updateUser, keywordsJson.get("cs"));
        addLangFrameKeywords(update, updateUser, keywordsJson.get("langFrame"));
        addFeatureByField(update, updateUser, keywordsJson.get("field"), keywordsJson.get("feature"));
//        addFeatureKeywords(update, keywordsJson.get("feature"));
//        addFieldKeywords(update, updateUser, keywordsJson.get("field"));
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

    private void addFeatureByField(Update updateCommit, Update updateUser, JsonNode fieldNode, JsonNode featureNode) {
        // DONE : 개별커밋에 field 저장, 사용자에 field 컬럼생성 및 count, feature keyword 저장
        /*
         * 1. 개별 feature 순회
         * 	1-1. Set<String>으로 메모리
         * 	1-2. commit에 하나씩 저장(asSet)
         *
         * 2. field 순회
         * 	2-1. commit 에 하나씩 저장 field
         * 	2-2. user에 field inc
         * 	2-3. user에 featureSet 저장 (null체크)
         * */

        Set<String> featureSet = null;

        // 개별 feature를 commitKeywords.featured 에 저장
        if (featureNode != null) {
            featureSet = new HashSet<>();
            for (JsonNode feature : featureNode) {
                String featureStr = feature.asText();
                featureSet.add(featureStr);
                updateCommit.addToSet("featured", featureStr);
            }
        }
        // feature가 있었다면 featureSet 데이터 메모리적재 완료됨.

        // userData에 filed별 feature 저장
        if (fieldNode != null) {
            for (JsonNode field : fieldNode) {
                // field명 (game, systemProgramming, ai, dataScience, database, mobile, webBackend, webFrontend)
                String fieldName = field.asText();

                updateCommit.addToSet("field", fieldName);
                updateUser.inc(fieldName);
                if (featureSet != null) {
                    for (String feature : featureSet) {
                        // key : gameSet, aiSet, ... / value : featureStr
                        updateUser.addToSet(fieldName + "Set", feature);
                    }
                }
            }

        }
    }


    private void addFieldKeywords(Update update, Update updateUser, JsonNode fieldNode) {
        if (fieldNode != null) {
            for (JsonNode field : fieldNode) {
                update.addToSet("field", field.asText());
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

    private void addMsgScore(Update updateUser, JsonNode msgScore) {
        if (msgScore != null) {
            for (JsonNode field : msgScore) {
                switch (field.asText()) {
                    case ("0"):
                        updateUser.inc("msgScore_0");
                        break;
                    case ("1"):
                        updateUser.inc("msgScore_1");
                        break;
                    case ("2"):
                        updateUser.inc("msgScore_2");
                        break;
                    case ("3"):
                        updateUser.inc("msgScore_3");
                        break;
                    case ("4"):
                        updateUser.inc("msgScore_4");
                        break;
                    case ("5"):
                        updateUser.inc("msgScore_5");
                        break;
                    case ("6"):
                        updateUser.inc("msgScore_6");
                        break;
                    case ("7"):
                        updateUser.inc("msgScore_7");
                        break;
                    case ("8"):
                        updateUser.inc("msgScore_8");
                        break;
                    case ("9"):
                        updateUser.inc("msgScore_9");
                        break;
                    case ("10"):
                        updateUser.inc("msgScore_10");
                        break;
                }
            }
        }
    }

}