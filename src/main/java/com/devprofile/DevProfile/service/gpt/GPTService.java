package com.devprofile.DevProfile.service.gpt;



import com.devprofile.DevProfile.entity.PatchEntity;
import com.devprofile.DevProfile.repository.PatchRepository;
import com.devprofile.DevProfile.service.commit.CommitKeywordsService;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

import java.util.Map;

@Service
public class GPTService {

    @Autowired
    private final CommitKeywordsService commitKeywordsService;

    @Autowired
    private final PatchRepository patchRepository;

    @Value("${gpt.url}")
    private String url;

    @Value("${gpt.secret}")
    private String key;

    public GPTService(CommitKeywordsService commitKeywordsService, PatchRepository patchRepository) {
        this.commitKeywordsService = commitKeywordsService;
        this.patchRepository = patchRepository;
    }

    @Transactional(readOnly = true)
    public void processAllEntities() {
        List<PatchEntity> patchEntities = patchRepository.findAll();
        patchEntities.forEach(this::generateKeyword);
    }

    public void generateKeyword(PatchEntity patchEntity){

        String patch = patchEntity.getPatch();
        String oid = patchEntity.getCommitOid();

        WebClient webClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION,"Bearer " + key)
                .build();

        String systemPrompt = "I am going to analyze the contents of a GitHub patch. You will receive the next given patch, and provide three to five keywords of 1-2 words each, describing what computer science knowledge is applied in this code(Not Language or framework).  Second Tell me what frameworks or languages this code uses. Also, provide  keywords that describes what feature this patch has modified(Up to 6 words). If the accuracy drops significantly, it's okay not to provide the second keyword. Also, produce the results in JSON format. First Key = cs, Second Key = framework and language, Third Key = feature Answer with English";
        // Create message objects
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", patch));

        webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of( "model", "gpt-3.5-turbo","messages" , messages))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnNext(jsonNode -> {
                    JsonNode content = jsonNode.get("choices").get("content");
                    commitKeywordsService.addCommitKeywords(oid, content);
                });
    }
}
