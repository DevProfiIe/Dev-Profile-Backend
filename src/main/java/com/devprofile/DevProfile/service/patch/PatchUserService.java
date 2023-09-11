package com.devprofile.DevProfile.service.patch;

import com.devprofile.DevProfile.entity.PatchEntity;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.devprofile.DevProfile.repository.PatchRepository;
import com.devprofile.DevProfile.service.rabbitmq.MessageSenderService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PatchUserService {

    private final CommitRepository commitRepository;
    private final PatchRepository patchRepository;
    private final WebClient webClient;
    private final MessageSenderService messageSenderService;

    private final Map<String, Integer> commitLengthMap = new HashMap<>();
    public PatchUserService(PatchRepository patchRepository, CommitRepository commitRepository,
                            @Qualifier("patchWebClient") WebClient webClient, MessageSenderService messageSenderService) {
        this.commitRepository = commitRepository;
        this.patchRepository = patchRepository;
        this.webClient = webClient;
        this.messageSenderService = messageSenderService;
    }


    public Mono<Void> savePatchs(String userName, String accessToken, Map<String, List<String>> repoOidsMap) {
        List<Mono<JsonNode>> requestMonos = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : repoOidsMap.entrySet()) {
            String repoName = entry.getKey();
            List<String> oids = entry.getValue();

            for (String oid : oids) {
                String commitDetailUrl = "https://api.github.com/repos/" + userName + "/" + repoName + "/commits/" + oid;

                Mono<JsonNode> requestMono = webClient.get()
                        .uri(commitDetailUrl)
                        .header("Authorization", "Bearer " + accessToken)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .onErrorResume(e -> {
                            log.error("Error while retrieving commit details for url: " + commitDetailUrl, e);
                            return Mono.empty();
                        });

                requestMonos.add(requestMono);
            }
        }

        return Flux.merge(requestMonos)
                .distinct(commitDetailResponse -> {
                    String sha = commitDetailResponse.get("sha").asText();
                    String filename = commitDetailResponse.at("/files/0/filename").asText();
                    return sha + "-" + filename;
                })
                .collectList()
                .flatMap(commitDetailResponses -> {
                    List<PatchEntity> patchesToSave = new ArrayList<>();
                    for (JsonNode commitDetailResponse : commitDetailResponses) {
                        if (commitDetailResponse.has("files")) {
                            for (JsonNode file : commitDetailResponse.get("files")) {
                                PatchEntity patchEntity = new PatchEntity();
                                String oid = commitDetailResponse.get("sha").asText();

                                if (file.has("filename")) patchEntity.setFileName(file.get("filename").asText());
                                if (file.has("contents_url"))
                                    patchEntity.setContentsUrl(file.get("contents_url").asText());
                                if (file.has("patch")) {
                                    String patch = file.get("patch").asText();
                                    int additions = 0;
                                    int deletions = 0;
                                    for (String line : patch.split("\n")) {
                                        if (line.startsWith("+")) {
                                            additions++;
                                        } else if (line.startsWith("-")) {
                                            deletions++;
                                        }
                                    }
                                    patchEntity.setDeletions(deletions);
                                    patchEntity.setAdditions(additions);
                                    patchEntity.setPatch(patch);
                                    patchEntity.setUserName(userName);

                                    Integer length = commitLengthMap.getOrDefault(oid, 0) + patch.length();
                                    commitLengthMap.put(oid, length);

                                    patchEntity.setCommitOid(oid);
                                    if (patchEntity.getPatch() != null) {
                                        patchesToSave.add(patchEntity);
                                    }
                                }
                            }
                        }
                    }

                    batchUpdateLengths();

                    List<String> patchesToCheck = new ArrayList<>();
                    for (PatchEntity patch : patchesToSave) {
                        patchesToCheck.add(patch.getPatch());
                    }
                    List<PatchEntity> existingPatches = patchRepository.findAllByPatchIn(patchesToCheck);

                    List<PatchEntity> finalPatchesToSave = new ArrayList<>(patchesToSave);
                    finalPatchesToSave.removeAll(existingPatches);

                    // 메세지 전송 시 에러 핸들링과 재시도
                    return Flux.fromIterable(finalPatchesToSave)
                            .flatMap(savedPatch -> {
                                return messageSenderService.PatchSendMessage(savedPatch)
                                        .onErrorResume(e -> {
                                            log.error("Error sending message for Patch: " + savedPatch.getCommitOid(), e);
                                            return Mono.empty();
                                        });
                            })
                            .then(Mono.empty());
                });
    }
    private void batchUpdateLengths() {
        for (Map.Entry<String, Integer> entry : commitLengthMap.entrySet()) {
            String commitOid = entry.getKey();
            Integer length = entry.getValue();
            commitRepository.updateLength(commitOid, length);
        }
        commitLengthMap.clear();
    }
}
