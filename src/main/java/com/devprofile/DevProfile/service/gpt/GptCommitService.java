//package com.devprofile.DevProfile.service.gpt;
//
//import com.devprofile.DevProfile.entity.CommitEntity;
//import com.devprofile.DevProfile.entity.CommitKeywordsEntity;
//import com.devprofile.DevProfile.repository.CommitKeywordsRepository;
//import com.devprofile.DevProfile.repository.CommitRepository;
//import com.devprofile.DevProfile.repository.PatchRepository;
//import com.devprofile.DevProfile.service.commit.CommitKeywordsService;
//import com.fasterxml.jackson.databind.JsonNode;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatusCode;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Mono;
//import reactor.util.retry.Retry;
//
//import java.time.Duration;
//import java.util.*;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class GptCommitService {
//
//    private final CommitKeywordsService commitKeywordsService;
//
//    // 커밋 단위로 gpt에게 질의 -> 데이터 저장은 다른 서비스로 넘김.
//    private final CommitRepository commitRepository;
//    private final CommitKeywordsRepository commitKeywordsRepository;
//
//    String systemPrompt =
//            """
//            You are programmer.
//            Respond with a score out of 10 on how appropriate the commit message is, comparing commitMessage to the patchKeywords list.
//            Regardless of the length of the response, please ensure that it is provided in JSON format and strictly conforms to the specified schema.
//            {"type":"object","properties":{"msgScore":{"type":"String", "items":"String"},}}
//            """;
//
//    @Value("${gpt.url}")
//    private String url;
//
//    @Value("${gpt.secret}")
//    private String key;
//
//
//    // 1개커밋oid -> 스코어링,
//    @Transactional(readOnly = true)
//    public void processOneCommit(String userName, String commitOid) {
//        CommitEntity commitEntity = commitRepository.findByCommitOid(commitOid).orElseThrow();
//        CommitKeywordsEntity commitKeywordsEntity = commitKeywordsRepository.findByOid(commitOid);
//        // username, commitEntity, commitKeywordsEntity
//        System.out.println("GptCOmmitService.processOneCommit()");
//        generateCommitMessageScore(userName, commitEntity, commitKeywordsEntity);
//    }
//
//    public void generateCommitMessageScore(String userName, CommitEntity commitEntity, CommitKeywordsEntity commitKeywordsEntity) {
//        System.out.println("GptCommitService.generateCommitMessageScore");
//        String commitMessage = commitEntity.getCommitMessage();
//        Set<String> features = commitKeywordsEntity.getFeatured();
//        if (commitMessage == null || features == null) {
//            System.out.println(commitEntity.toString());
//            System.out.println(commitKeywordsEntity.toString());
//            return;
//        }
//        WebClient webClient = createWebClient();
//
//		String content = "commitMessage:" + commitMessage + ", patchKeywords:" + features.toString();
//
//        System.out.println("generateCommitMessageScore.content : " + content);
//        List<Map<String, String>> messages = createMessageObjects(content);
//
//        try {
//            JsonNode jsonNode = postToGptService(webClient, messages);
//            processJsonNode(jsonNode, userName, commitEntity.getCommitOid());
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        }
//    }
//
//    private WebClient createWebClient() {
//        return WebClient.builder()
//                .baseUrl(url)
//                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key)
//                .build();
//    }
//
//    private List<Map<String, String>> createMessageObjects(String patch) {
//        List<Map<String, String>> messages = new ArrayList<>();
//        messages.add(Map.of("role", "system", "content", systemPrompt));
//        messages.add(Map.of("role", "user", "content", patch));
//        return messages;
//    }
//
//    private JsonNode postToGptService(WebClient webClient, List<Map<String, String>> messages) throws Exception {
//        return webClient.post()
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(Map.of("model", "gpt-3.5-turbo", "messages", messages, "temperature", 0.33, "top_p", 0.65))
//                .retrieve()
//                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
//                        Mono.error(new Exception("Client Error: " + clientResponse.statusCode())))
//                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
//                        Mono.error(new Exception("Server Error: " + clientResponse.statusCode())))
//                .bodyToMono(JsonNode.class)
//                .retryWhen(Retry.backoff(5, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
//                .block();
//    }
//
//    private void processJsonNode(JsonNode jsonNode, String userName, String oid) {
//        JsonNode choices = jsonNode.get("choices");
//        if (choices != null && choices.isArray() && choices.size() > 0) {
//            JsonNode content = choices.get(0).get("message").get("content");
//            commitKeywordsService.processMsgScore(userName, content.toString()).subscribe();
//        }
//    }
//}
