package com.devprofile.DevProfile.service.commit;


import com.devprofile.DevProfile.entity.CommitKeywordsEntity;
import com.devprofile.DevProfile.entity.UserDataEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

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
        Query query = new Query(Criteria.where("oid").is(oid));
        Query queryUser = new Query(Criteria.where("userName").is(userName));
        Update update = new Update();
        Update updateUser = new Update();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode keywordsJson = mapper.readTree(keywords);
            // 이후 jsonNode 객체를 사용할 수 있습니다.
            System.out.println("keywordsJson = " + keywordsJson);
            JsonNode css = keywordsJson.get("cs");
            if (css != null) {
                for (JsonNode cs : css) {
                    update.addToSet("cs", cs.asText());
                    updateUser.addToSet("keywordSet", cs.asText());
                    System.out.println("cs = " + cs);
                }
            }
            JsonNode langFrames = keywordsJson.get("langFrame");
            System.out.println("langFrames = " + langFrames);
            if (langFrames != null) {
                for (JsonNode langFrame : langFrames) {
                    update.addToSet("langFramework", langFrame.asText());
                    updateUser.addToSet("keywordSet", langFrame.asText());
                }
            }

            if (keywordsJson.get("feature") != null) {
                update.addToSet("featured", keywordsJson.get("feature").asText());
                updateUser.addToSet("keywordSet", keywordsJson.get("feature").asText());
            }

            mongoTemplate.upsert(queryUser, updateUser, UserDataEntity.class);
            mongoTemplate.upsert(query, update, CommitKeywordsEntity.class);
            return Mono.empty();
        }
        catch (Exception e) {
            // 문자열이 유효한 JSON이 아닌 경우 오류 처리를 수행합니다.
            e.printStackTrace();
            return null;
        }
    }
}
