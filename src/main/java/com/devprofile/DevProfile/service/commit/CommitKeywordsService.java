package com.devprofile.DevProfile.service.commit;

import com.devprofile.DevProfile.entity.CommitKeywordsEntity;
import com.devprofile.DevProfile.entity.FrameworkEntity;
import com.devprofile.DevProfile.entity.UserDataEntity;
import com.devprofile.DevProfile.entity.WordEntity;
import com.devprofile.DevProfile.repository.FrameworkRepository;
import com.devprofile.DevProfile.repository.WordRepository;
import com.devprofile.DevProfile.service.search.SparqlService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommitKeywordsService {

    private final MongoTemplate mongoTemplate;
    private final WordRepository wordRepository;
    private final FrameworkRepository frameworkRepository;
    private final SparqlService sparqlService;



    public WordEntity getClosestWord(String inputWord) {

        inputWord = inputWord.toLowerCase();
        char firstChar = inputWord.charAt(0);
        List<WordEntity> candidateWords = wordRepository.findByFirstChar(firstChar);

        WordEntity closestWord = null;
        double maxSimilarity = 0.65;

        for (WordEntity wordEntity : candidateWords) {
            double currentDistance = StringUtils.getJaroWinklerDistance(inputWord, wordEntity.getKeyword().toLowerCase());
            if (maxSimilarity <= currentDistance) {
                maxSimilarity = currentDistance;
                closestWord = wordEntity;
            }
        }
        return closestWord;
    }

    @Autowired
    public CommitKeywordsService(MongoTemplate mongoTemplate, WordRepository wordRepository, FrameworkRepository frameworkRepository, SparqlService sparqlService) {
        this.mongoTemplate = mongoTemplate;
        this.wordRepository = wordRepository;
        this.frameworkRepository = frameworkRepository;
        this.sparqlService = sparqlService;
    }

    public FrameworkEntity getClosestFramework(String inputWord) {

        inputWord = inputWord.toLowerCase();

        List<FrameworkEntity> wordEntities = frameworkRepository.findAll();
        FrameworkEntity closestFramework = null;
        double maxSimilarity = 0.85;
        for(FrameworkEntity framework : wordEntities){
            double similarity = StringUtils.getJaroWinklerDistance(framework.getFrameworkName().toLowerCase(), inputWord);
            System.out.println("similarity = " + similarity);
            if(maxSimilarity <= similarity){
                maxSimilarity = similarity;
                closestFramework = framework;
            }
        }
        return closestFramework;
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
            update.addToSet("userName", userName);
            Query query = new Query(Criteria.where("oid").is(oid));
            Query queryUser = new Query(Criteria.where("userName").is(userName));
            mongoTemplate.upsert(query, update, CommitKeywordsEntity.class);
            mongoTemplate.upsert(queryUser, updateUser, UserDataEntity.class);
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
        List<String> featureList = addFeatureKeywords(update, keywordsJson.get("feature"));
        addFieldKeywords(featureList, updateUser, update,keywordsJson.get("field"));
    }

    private void addCsKeywords(Update update, Update updateUser, JsonNode csNode) {
        if (csNode != null) {
            for (JsonNode cs : csNode) {
                WordEntity closestWord = getClosestWord(cs.asText());
                if(closestWord == null) continue;
                String keyword = sparqlService.findRedirect(closestWord);
                System.out.println("keyword = " + keyword);
                update.addToSet("cs", keyword);
                updateUser.addToSet("keywordSet", keyword);
            }
        }
    }

    private void addLangFrameKeywords(Update update, Update updateUser, JsonNode langFrameNode) {
        if (langFrameNode != null) {
            for (JsonNode langFrame : langFrameNode) {
                FrameworkEntity framework= getClosestFramework(langFrame.asText());
                if(framework == null) continue;
                update.addToSet("langFramework", framework.getFrameworkName());
                updateUser.addToSet("keywordSet", framework.getFrameworkName());
            }
        }
    }

    private List<String> addFeatureKeywords(Update update, JsonNode featureNode) {
        List<String> featureList = new ArrayList<>();
        if (featureNode != null) {
            for (JsonNode feature : featureNode) {
                update.addToSet("featured", feature.asText().toLowerCase());
                featureList.add(feature.asText().toLowerCase());
            }
        }
        return featureList;
    }

    private void addFieldKeywords(List<String> featureList,Update updateUser, Update update, JsonNode fieldNode) {
        if (fieldNode != null) {
            for (JsonNode field : fieldNode) {
                switch (field.asText()) {
                    case ("Game"):
                        update.addToSet("field", "game");
                        for(String feature: featureList) updateUser.addToSet("gameSet",feature);
                        break;
                    case ("Web Backend"):
                        update.addToSet("field", "webBackend");
                        for(String feature: featureList) updateUser.addToSet("webBackendSet", feature);
                        break;
                    case ("Web Frontend"):
                        update.addToSet("field", "webFrontend");
                        for(String feature: featureList) updateUser.addToSet("webFrontendSet", feature);
                        break;
                    case ("Database"):
                        update.addToSet("field", "database");
                        for(String feature: featureList) updateUser.addToSet("databaseSet", feature);
                        break;
                    case ("Mobile"):
                        update.addToSet("field", "mobile");
                        for(String feature: featureList) updateUser.addToSet("mobileSet", feature);
                        break;
                    case ("System Programming"):
                        update.addToSet("field", "systemProgramming");
                        for(String feature: featureList) updateUser.addToSet("systemProgrammingSet", feature);
                        break;
                    case ("AI"):
                        update.addToSet("field", "ai");
                        for(String feature: featureList) updateUser.addToSet("aiSet", feature);
                        break;
                }
            }
        }
    }

    private void addMsgScore (Update updateUser, JsonNode msgScore) {
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