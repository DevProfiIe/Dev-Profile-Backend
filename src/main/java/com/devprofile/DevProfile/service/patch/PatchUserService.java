package com.devprofile.DevProfile.service.patch;

import com.devprofile.DevProfile.entity.PatchEntity;
import com.devprofile.DevProfile.repository.PatchRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PatchUserService {

    private final PatchRepository patchRepository;
    private final WebClient webClient;

    public PatchUserService(PatchRepository patchRepository, @Qualifier("patchWebClient") WebClient webClient) {
        this.patchRepository = patchRepository;
        this.webClient = webClient;
    }

    public void savePatchs(String userName, String accessToken, Map<String, List<String>> repoOidsMap) {
        for (Map.Entry<String, List<String>> entry : repoOidsMap.entrySet()) {
            String repoName = entry.getKey();
            List<String> oids = entry.getValue();

            for (String oid : oids) {
                String commitDetailUrl = "https://api.github.com/repos/" + userName + "/" + repoName + "/commits/" + oid;

                webClient.get()
                        .uri(commitDetailUrl)
                        .header("Authorization", "Bearer " + accessToken)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .subscribe(commitDetailResponse -> {
                            if (commitDetailResponse.has("files")) {
                                for (JsonNode file : commitDetailResponse.get("files")) {
                                    PatchEntity patchEntity = new PatchEntity();
                                    if (file.has("filename")) patchEntity.setFileName(file.get("filename").asText());
                                    if (file.has("raw_url")) patchEntity.setRawUrl(file.get("raw_url").asText());
                                    if (file.has("contents_url")) patchEntity.setContentsUrl(file.get("contents_url").asText());
                                    if (file.has("patch")) patchEntity.setPatch(file.get("patch").asText());
                                    patchEntity.setCommitOid(oid);

                                    patchRepository.save(patchEntity);
                                }
                            }
                        }, error -> log.error("Error fetching commit detail: ", error));
            }
        }
    }
}

