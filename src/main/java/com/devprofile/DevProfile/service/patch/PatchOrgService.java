package com.devprofile.DevProfile.service.patch;

import com.devprofile.DevProfile.entity.PatchEntity;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.devprofile.DevProfile.repository.PatchRepository;
import com.devprofile.DevProfile.service.rabbitmq.MessageOrgSenderService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PatchOrgService {

    private final PatchRepository patchRepository;
    private final WebClient webClient;
    private final CommitRepository commitRepository;
    private final MessageOrgSenderService messageOrgSenderService;

    public PatchOrgService(PatchRepository patchRepository, @Qualifier("patchWebClient") WebClient webClient,
                           CommitRepository commitRepository, MessageOrgSenderService messageOrgSenderService) {
        this.patchRepository = patchRepository;
        this.webClient = webClient;
        this.commitRepository = commitRepository;
        this.messageOrgSenderService = messageOrgSenderService;
    }


    public Mono<Void> savePatchs(String accessToken, Map<String, Map<String, List<String>>> orgRepoCommits) {
        List<Mono<JsonNode>> requestMonos = new ArrayList<>();

        for (Map.Entry<String, Map<String, List<String>>> orgEntry : orgRepoCommits.entrySet()) {
            String orgName = orgEntry.getKey();
            Map<String, List<String>> repoCommits = orgEntry.getValue();

            for (Map.Entry<String, List<String>> repoEntry : repoCommits.entrySet()) {
                String repoName = repoEntry.getKey();
                List<String> oids = repoEntry.getValue();

                for (String oid : oids) {
                    String commitDetailUrl = "https://api.github.com/repos/" + orgName + "/" + repoName + "/commits/" + oid;

                    Mono<JsonNode> requestMono = webClient.get()
                            .uri(commitDetailUrl)
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(JsonNode.class);

                    requestMonos.add(requestMono);
                }
            }
        }

        return Flux.merge(requestMonos)
                .flatMap(response -> processResponse(response))
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(5)))
                .onErrorResume(WebClientResponseException.class, error -> {
                    log.error("Error while fetching commit detail: {}", error.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    private Mono<Void> processResponse(JsonNode response) {
        if (!response.has("sha")) {
            return Mono.empty();
        }
        String oid = response.get("sha").asText();
        return Mono.fromCallable(() -> commitRepository.findByCommitOid(oid).orElseThrow())
                .flatMap(commitEntity -> {
                    List<Mono<Void>> operations = new ArrayList<>();
                    if (response.has("files")) {
                        for (JsonNode file : response.get("files")) {
                            PatchEntity patchEntity = new PatchEntity();
                            if (file.has("filename"))
                                patchEntity.setFileName(file.get("filename").asText());
                            if (file.has("contents_url"))
                                patchEntity.setContentsUrl(file.get("contents_url").asText());
                            if (file.has("patch")) {
                                String patch = file.get("patch").asText();
                                patchEntity.setPatch(patch);
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
                                patchEntity.setCommitOid(oid);
                                patchEntity.setUserName(commitEntity.getUserName());

                                commitRepository.updateLength(oid, patch.length());
                            }

                            if (patchEntity.getPatch() != null) {
                                List<PatchEntity> existingPatches = patchRepository.findAllByPatch(patchEntity.getPatch());
                                if (existingPatches.isEmpty()) {
                                    Mono<Void> saveAndSendMono = Mono.fromCallable(() -> patchRepository.save(patchEntity))
                                            .then(messageOrgSenderService.orgPatchSendMessage(patchEntity))
                                            .then();
                                    operations.add(saveAndSendMono);
                                }
                            }
                        }
                    }
                    return Flux.merge(operations).then();
                })
                .doOnError(error -> log.error("Error fetching commit detail: ", error))
                .then();
    }

    private Mono<PatchEntity> savePatchEntity(PatchEntity patchEntity) {
        return Mono.fromCallable(() -> patchRepository.save(patchEntity));
    }
}