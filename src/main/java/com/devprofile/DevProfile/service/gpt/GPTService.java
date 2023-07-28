package com.devprofile.DevProfile.service.gpt;


import com.devprofile.DevProfile.entity.PatchEntity;
import com.devprofile.DevProfile.repository.PatchRepository;
import com.devprofile.DevProfile.service.commit.CommitKeywordsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class GPTService {


    private final CommitKeywordsService commitKeywordsService;


    private final PatchRepository patchRepository;

    String systemPrompt = "Answer in English.\n" +
            "1.cs: Provide three to five keywords, each consisting of 1-2 words, that describe the computer science principles or concepts applied in this code, excluding specific languages or frameworks.\n" +
            "2.frameLang: Provide the framework used in this code.\n" +
            "3.feature:Provide a con                                                                                                                                                cise 1-2 line description of the feature implemented by this provided code.\n" +
            "4.field: Select the most relevant keyword from the following list: Game, System Programming, AI, Data Science, Database, Mobile, Web Backend, Web Frontend, Document. If the accuracy significantly decreases, it's acceptable to omit the 3rd and 4th keywords. Regardless of the length of the response, please ensure that it is provided in JSON format and strictly conforms to the specified schema.\n" +
            "```\n" +
            "{\"type\":\"object\",\"properties\":{\"cs\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"langFrame\":{\"type\":\"array\",\"items”:{“type\":\"string\"}},\"feature\":{\"type”:”array\",\"items\":{\"type\":\"string\"}},\"field\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}}}}";

    @Value("${gpt.url}")
    private String url;

    @Value("${gpt.secret}")
    private String key;



    @Transactional(readOnly = true)
    public void processAllEntities(String userName) {
        List<PatchEntity> patchEntities = patchRepository.findAll();
        patchEntities.forEach(patchEntity -> this.generateKeyword(userName, patchEntity));
    }

    public void generateKeyword(String userName, PatchEntity patchEntity) {
        String patch = patchEntity.getPatch();
        if (patch == null) {
            return;
        }
        WebClient webClient = createWebClient();


        List<Map<String, String>> messages = createMessageObjects(patch);

        try {
            JsonNode jsonNode = postToGptService(webClient, messages);
            processJsonNode(jsonNode, userName, patchEntity.getCommitOid());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private WebClient createWebClient() {
        return WebClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                .build();
    }

    private List<Map<String, String>> createMessageObjects(String patch) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", patch));
        return messages;
    }

    private JsonNode postToGptService(WebClient webClient, List<Map<String, String>> messages) throws Exception {
        return webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("model", "gpt-3.5-turbo", "messages", messages, "temperature", 0.33,"top_p",0.65))
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
}