package com.devprofile.DevProfile.service.commit;

import com.devprofile.DevProfile.entity.PatchEntity;
import com.devprofile.DevProfile.repository.PatchRepository;
import com.devprofile.DevProfile.service.gpt.GPTService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PatchService {

    private final PatchRepository patchRepository;
    private final WebClient webClient;

    private final GPTService gptService;

    public PatchService(PatchRepository patchRepository, @Qualifier("patchWebClient") WebClient webClient, GPTService gptService) {
        this.patchRepository = patchRepository;
        this.webClient = webClient;
        this.gptService = gptService;
    }

    public void extractAndSavePatchs(String userName, String accessToken, Map<String, List<String>> repoOidsMap, Map<String,Map<String, List<String>>> orgOidsMap, List<String> orgNames) {
        for (Map.Entry<String, List<String>> entry : repoOidsMap.entrySet()) {
            String repoName = entry.getKey();
            List<String> oids = entry.getValue();

            handlePatchExtraction(userName, accessToken, repoName, oids);
        }

        for (Map.Entry<String, Map<String, List<String>>> entry : orgOidsMap.entrySet()) {
            String orgName = entry.getKey();
            Map<String,List<String>> repoNameMap = entry.getValue();

            for (Map.Entry<String, List<String>> orgEntry : repoNameMap.entrySet()) {
                String repoName = orgEntry.getKey();
                List<String> oids = orgEntry.getValue();

                orgHandlePatchExtraction(accessToken, repoName, oids, orgName);
            }
        }

    }

    private void handlePatchExtraction(String userName, String accessToken, String repoName, List<String> oids) {
        Flux.fromIterable(oids)
                .flatMap(oid -> {
                    String commitDetailUrl = "https://api.github.com/repos/" + userName + "/" + repoName + "/commits/" + oid;

                    return webClient.get()
                            .uri(commitDetailUrl)
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .map(commitDetailResponse -> {
                                List<PatchEntity> patchesToSave = new ArrayList<>();
                                if (commitDetailResponse.has("files")) {
                                    for (JsonNode file : commitDetailResponse.get("files")) {
                                        PatchEntity patchEntity = new PatchEntity();
                                        if (file.has("filename")) patchEntity.setFileName(file.get("filename").asText());
                                        if (file.has("raw_url")) patchEntity.setRawUrl(file.get("raw_url").asText());
                                        if (file.has("contents_url")) patchEntity.setContentsUrl(file.get("contents_url").asText());
                                        if (file.has("patch")) patchEntity.setPatch(file.get("patch").asText());
                                        patchEntity.setCommitOid(oid);
                                        patchesToSave.add(patchEntity);
                                    }
                                }
                                return patchesToSave;
                            });
                })
                .doOnError(error -> {
                    log.error(error.getMessage(), error);
                })
                .subscribe(patchesToSave -> {
                    if (!patchesToSave.isEmpty()) {
                        savePatchInfo(patchesToSave);
                    }
                });
    }

    private void orgHandlePatchExtraction(String accessToken, String repoName, List<String> oids, String orgName) {
        Flux.fromIterable(oids)
                .flatMap(oid -> {
                    String commitDetailUrl = "https://api.github.com/repos/" + orgName + "/" + repoName + "/commits/" + oid;

                    return webClient.get()
                            .uri(commitDetailUrl)
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .map(commitDetailResponse -> {
                                List<PatchEntity> patchesToSave = new ArrayList<>();
                                if (commitDetailResponse.has("files")) {
                                    for (JsonNode file : commitDetailResponse.get("files")) {
                                        PatchEntity patchEntity = new PatchEntity();
                                        if (file.has("filename")) patchEntity.setFileName(file.get("filename").asText());
                                        if (file.has("raw_url")) patchEntity.setRawUrl(file.get("raw_url").asText());
                                        if (file.has("contents_url")) patchEntity.setContentsUrl(file.get("contents_url").asText());
                                        if (file.has("patch")){patchEntity.setPatch(file.get("patch").asText());}
                                        patchEntity.setCommitOid(oid);


                                        patchesToSave.add(patchEntity);
                                    }
                                }
                                return patchesToSave;
                            });
                })
                .doOnError(error -> {
                    log.error(error.getMessage(), error);
                })
                .subscribe(patchesToSave -> {
                    if (!patchesToSave.isEmpty()) {
                        savePatchInfo(patchesToSave);
                    }
                });
    }

    @Transactional
    public void savePatchInfo(List<PatchEntity> patches) {
        try {
            patchRepository.saveAll(patches);
            patchRepository.flush();
        } catch (DataAccessException ex) {
            log.error("Error occurred while saving patches", ex);
        }
    }
}
