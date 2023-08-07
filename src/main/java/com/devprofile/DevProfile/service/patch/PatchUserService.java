package com.devprofile.DevProfile.service.patch;

import com.devprofile.DevProfile.entity.CommitEntity;
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
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PatchUserService {

    private final CommitRepository commitRepository;
    private final PatchRepository patchRepository;
    private final WebClient webClient;
    private final MessageSenderService messageSenderService;

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
                .flatMap(commitDetailResponse -> {
                    List<Mono<Void>> operations = new ArrayList<>();
                    if (commitDetailResponse.has("files")) {
                        for (JsonNode file : commitDetailResponse.get("files")) {
                            PatchEntity patchEntity = new PatchEntity();
                            String oid = commitDetailResponse.get("sha").asText();
                            CommitEntity commitEntity = commitRepository.findByCommitOid(oid)
                                    .orElseThrow();

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

                                commitRepository.updateLength(oid, patch.length());

                                patchEntity.setCommitOid(oid);
                                if (patchEntity.getPatch() != null) {
                                    List<PatchEntity> existingPatches = patchRepository.findAllByPatch(patchEntity.getPatch());
                                    if (existingPatches.isEmpty()) {
                                        operations.add(Mono.fromCallable(() -> {
                                            patchRepository.saveAndFlush(patchEntity);
                                            return null;
                                        }).then(messageSenderService.PatchSendMessage(patchEntity)).then());
                                    }
                                }
                            }
                        }
                    }
                    return Flux.merge(operations).then();
                })
                .doOnError(error -> log.error("Error fetching commit detail: ", error))
                .then();
    }
}