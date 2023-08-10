package com.devprofile.DevProfile.service.gpt;


import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.entity.CommitKeywordsEntity;
import com.devprofile.DevProfile.entity.PatchEntity;
import com.devprofile.DevProfile.entity.UserDataEntity;
import com.devprofile.DevProfile.repository.CommitKeywordsRepository;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.devprofile.DevProfile.repository.PatchRepository;
import com.devprofile.DevProfile.repository.UserDataRepository;
import com.devprofile.DevProfile.service.commit.CommitKeywordsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class GptPatchService {


    private final CommitKeywordsService commitKeywordsService;

    private final PatchRepository patchRepository;

    private final CommitKeywordsRepository commitKeywordsRepository;

    private final CommitRepository commitRepository;

    private final MongoTemplate mongoTemplate;
    private final UserDataRepository userDataRepository;

    String systemPrompt =
            """
             Answer in English.
             1.cs: Provide three to five keywords, each consisting of a word, that describe the computer science principles or concepts applied in this code, excluding specific languages or frameworks.
             2.langFrame: Provide the framework used in this code.
             3.feature:Provide a concise description of the feature implemented by this provided code in 10-20 characters.
             4.field: Select the most relevant keyword from the following list: Game, System Programming, AI, Database, Mobile, Web Backend, Web Frontend
             If the accuracy significantly decreases, it's acceptable to omit the 3rd and 4th keywords
             Write keywords only the label of the entity in dbpedia.     \s
             Regardless of the length of the response, please ensure that it is provided in JSON format and strictly conforms to the specified schema.
             {"type":"object","properties":{"cs":{"type":"array", "items":{"type":"string"}},"langFrame":{"type":"array", "items":{"type":"string"}},"feature":{"type":"array","items":{"type":"string"}},"field":{"type":"array","items":{"type":"string"}}}}
            """;
    String systemPromptKeyWordAnalyze =
            """
            당신은 새로운 개발자를 고용하는 고용 관리자입니다. 아래 키워드는 한 개발자가 사용한 기술과 이와 연관된 patch의 갯수입니다. 이 키워드로 신입 개발자를 4~6문장으로 평가한다. "이 개발자는"으로 시작해줘. \s
            재밌는 이름 지어서 title에 적어줘.\s
            둘다 한글로 뽑아줘, 형식은
            {title: String
            content:String}
            """;

    @Value("${gpt.url}")
    private String url;

    @Value("${gpt.secret}")
    private String key;


    public void processAllEntities(String userName) {
        List<CommitEntity> commitEntities = commitRepository.findAll();
        List<PatchEntity> patchEntities;
        Query query = new Query(Criteria.where("userName").is(userName));

        UserDataEntity userDataEntity = new UserDataEntity();
        userDataEntity.setUserName(userName);
        userDataEntity.setCs(new HashMap<>());
        userDataRepository.save(userDataEntity);
        for(CommitEntity commitEntity : commitEntities){
            patchEntities = patchRepository.findByCommitOid(commitEntity.getCommitOid());
            if(patchEntities.isEmpty())continue;
            patchEntities.forEach(patchEntity -> this.generateKeyword(userName, patchEntity));
            CommitKeywordsEntity commitKeywords =commitKeywordsRepository.findByOid(commitEntity.getCommitOid());
            if(commitKeywords == null) continue;
            Set<String> fields= commitKeywords.getField();
            Update update = new Update();
            if(fields == null) continue;
            for(String field : fields) update.inc(field);
            mongoTemplate.updateFirst(query, update, UserDataEntity.class);
        }
    }

    public void generateKeyword(String userName, PatchEntity patchEntity) {
        String patch = patchEntity.getPatch();
        if (patch == null) {
            return;
        }
        WebClient webClient = createWebClient();
        List<Map<String, String>> messages = createMessageObjects(patch, systemPrompt);

        try {
            JsonNode jsonNode = postToGptService(webClient, messages);
            processJsonNode(jsonNode, userName, patchEntity.getCommitOid());

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    public void generateSentence(String userName) throws Exception {
        Map<String, Integer> keywordSet= userDataRepository.findByUserName(userName).getCs();
        WebClient webClient = createWebClient();
        List<Map<String, String>> messages = createMessageObjects(keywordSet.toString(), systemPromptKeyWordAnalyze);
        System.out.println("keywordSet = " + keywordSet);
        JsonNode node = postToGptService16K(webClient,messages);
        JsonNode choices = node.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode content = choices.get(0).get("message").get("content");
            UserDataEntity userDataEntity = userDataRepository.findByUserName(userName);
            Map<String, String> titleSentence=processJsonNodeAnalyze(content, userName);
            userDataEntity.setUserKeywordAnalyze(titleSentence.get("userKeywordAnalyze"));
            userDataEntity.setUserTitle(titleSentence.get("userTitle"));
            userDataRepository.save(userDataEntity);
        }
    }

    private WebClient createWebClient() {
        return WebClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                .build();
    }

    private List<Map<String, String>> createMessageObjects(String patch ,String system) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));
        messages.add(Map.of("role", "user", "content", patch));
        return messages;
    }

    private JsonNode postToGptService(WebClient webClient, List<Map<String, String>> messages) throws Exception {
        return webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("model", "gpt-3.5-turbo", "messages", messages, "temperature", 0.33, "top_p", 0.65))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        Mono.error(new Exception("Client Error: " + clientResponse.statusCode())))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        Mono.error(new Exception("Server Error: " + clientResponse.statusCode())))
                .bodyToMono(JsonNode.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(5)))
                .block();
    }

    private JsonNode postToGptService16K(WebClient webClient, List<Map<String, String>> messages) throws Exception {
        return webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("model", "gpt-3.5-turbo-16k", "messages", messages, "temperature", 1, "top_p", 1))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        Mono.error(new Exception("Client Error: " + clientResponse.statusCode())))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        Mono.error(new Exception("Server Error: " + clientResponse.statusCode())))
                .bodyToMono(JsonNode.class)
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                .block();
    }

    private void processJsonNode(JsonNode jsonNode, String userName, String oid) {
        JsonNode choices = jsonNode.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode content = choices.get(0).get("message").get("content");
            commitKeywordsService.addCommitKeywords(userName, oid, content.toString()).subscribe();
        }
    }

    private Map<String, String> processJsonNodeAnalyze(JsonNode jsonNode, String userName) {
        JsonNode choices = jsonNode.get("choices");
        System.out.println("choices = " + choices);
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode content = choices.get(0).get("message").get("content");
            try {
                return commitKeywordsService.addSentenceTitle(userName, content.toString());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}