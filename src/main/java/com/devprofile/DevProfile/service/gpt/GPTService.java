package com.devprofile.DevProfile.service.gpt;



import com.devprofile.DevProfile.entity.PatchEntity;
import com.devprofile.DevProfile.repository.PatchRepository;
import com.devprofile.DevProfile.service.commit.CommitKeywordsService;
import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class GPTService {


    private final CommitKeywordsService commitKeywordsService;


    private final PatchRepository patchRepository;

    @Value("${gpt.url}")
    private String url;

    @Value("${gpt.secret}")
    private String key;




    @Transactional(readOnly = true)
    public void processAllEntities(String userName) {
        List<PatchEntity> patchEntities = patchRepository.findAll();
        patchEntities.forEach(patchEntity -> this.generateKeyword(userName, patchEntity));
    }

    public void generateKeyword(String userName, PatchEntity patchEntity){
        String patch = patchEntity.getPatch();
        String oid = patchEntity.getCommitOid();
        if(patch == null){
            return;
        }
        WebClient webClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION,"Bearer " + key)
                .build();

        String systemPrompt = "Answer in English. Analyze the contents of a GitHub patch.\n" +
                "1.cs: Provide three to five keywords of 1~2 words each, describing what computer science knowledge is applied in this code (Not Language or framework).  \n" +
                "2.frameLang: Provide frameworks or languages this code uses.\n" +
                "3.feature: Provide 1~3 keywords that describes what feature this patch has modified(Up to 6 words). \n" +
                "4.field: Pick the nearest keyword in these Keywords: Game, Algorithm, System Programming, AI, Data Science, Database, Mobile, Web Backend, Web Frontend, Document.\n" +
                "If the accuracy drops significantly, it's okay not to provide 3 and 4 keywords.\n" +
                "Produce the results in JSON format. \n" +
                "Respond in the following schema.\n" +
                "```\n" +
                "{\"type\": \"object\",\"properties\": {\"cs\": {\"type\": \"array\", \"items\": { \"type\": \"string\"}},\"langFrame\": {\"type\": \"array\",\"items\": { \"type\": \"string\"} },\"feature\": {\"type\": \"array\", \"items\": { \"type\": \"string\" } },\"field\": {\"type\": \"array\",\"items\": {\"type\": \"string\"}}}}\n" +
                "```";
        // Create message objects
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", patch));

        try {
            JsonNode jsonNode = webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of( "model", "gpt-3.5-turbo","messages" , messages, "temperature", 0.4))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            Mono.error(new Exception("Client Error: " + clientResponse.statusCode())))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            Mono.error(new Exception("Server Error: " + clientResponse.statusCode())))
                    .bodyToMono(JsonNode.class)
                    .retryWhen(Retry.backoff(5, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                    .block();
            System.out.println("jsonNode = " + jsonNode);
            JsonNode choices = jsonNode.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode content = choices.get(0).get("message").get("content");
                commitKeywordsService.addCommitKeywords(userName, oid, content.toString()).block();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
