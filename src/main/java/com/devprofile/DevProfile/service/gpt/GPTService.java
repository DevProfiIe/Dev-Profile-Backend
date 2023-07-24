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
import reactor.core.publisher.Mono;

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
    public void processAllEntities(String userName) {
        List<PatchEntity> patchEntities = patchRepository.findAll();
        patchEntities.forEach(patchEntity -> this.generateKeyword(userName, patchEntity));
    }

    public void generateKeyword(String userName, PatchEntity patchEntity){

        String patch = patchEntity.getPatch();
//        String oid = patchEntity.getid();
        String oid = "";
        WebClient webClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION,"Bearer " + key)
                .build();

        String systemPrompt = "I am going to analyze the contents of a GitHub patch. You will receive the next given patch, and provide three to five keywords of 1-2 words each, describing what computer science knowledge is applied in this code(Not Language or framework).  Second Tell me what frameworks or languages this code uses. Also, provide  keywords that describes what feature this patch has modified(Up to 6 words). If the accuracy drops significantly, it's okay not to provide the second keyword. Pick only one keyword in these seven Keywords: Game, Embedded systems, AI, Data Science, Database, Mobile, Web Backend, Web Frontend. Also, produce the results in JSON format. First Key = cs, Second Key = framework and language, Third Key = feature, Fourth Key = field. Answer with English";
        // Create message objects
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", patch));

        webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of( "model", "gpt-3.5-turbo","messages" , messages))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(jsonNode -> {
                    JsonNode choices = jsonNode.get("choices");
                    if (choices != null && choices.isArray() && choices.size() > 0) {
                        JsonNode content = choices.get(0).get("message").get("content");
                        return commitKeywordsService.addCommitKeywords(userName, oid, content);
                    }
                    return Mono.empty();
                }).subscribe();
    }
}
