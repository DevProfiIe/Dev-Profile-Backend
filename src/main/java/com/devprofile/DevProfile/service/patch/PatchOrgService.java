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
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PatchOrgService {

    private final PatchRepository patchRepository;
    private final WebClient webClient;
    private final CommitRepository commitRepository;
    private final MessageSenderService messageSenderService;

    public PatchOrgService(PatchRepository patchRepository, @Qualifier("patchWebClient") WebClient webClient,
                           CommitRepository commitRepository, MessageSenderService messageSenderService) {
        this.patchRepository = patchRepository;
        this.webClient = webClient;
        this.commitRepository = commitRepository;
        this.messageSenderService = messageSenderService;
    }


    public Mono<Void> savePatchs(String accessToken, Map<String, Map<String, List<String>>> orgRepoCommits) {
        return Flux.fromIterable(orgRepoCommits.entrySet())
                .flatMap(orgEntry -> {
                    String orgName = orgEntry.getKey();
                    Map<String, List<String>> repoCommits = orgEntry.getValue();
                    return Flux.fromIterable(repoCommits.entrySet())
                            .flatMap(repoEntry -> {
                                String repoName = repoEntry.getKey();
                                List<String> oids = repoEntry.getValue();
                                return Flux.fromIterable(oids)
                                        .flatMap(oid -> {
                                            String commitDetailUrl = "https://api.github.com/repos/" + orgName + "/" + repoName + "/commits/" + oid;
                                            return webClient.get()
                                                    .uri(commitDetailUrl)
                                                    .header("Authorization", "Bearer " + accessToken)
                                                    .retrieve()
                                                    .bodyToMono(JsonNode.class)
                                                    .flatMap(response -> processResponse(response, oid))
                                                    .then();
                                        });
                            });
                })
                .then();
    }

    private Mono<Void> processResponse(JsonNode response, String oid) {
        return Mono.fromCallable(() -> commitRepository.findByCommitOid(oid).orElseThrow())
                .flatMapMany(commitEntity -> {
                    List<Mono<Void>> monos = new ArrayList<>();
                    if(response.has("files")) {
                        for (JsonNode file : response.get("files")) {
                            PatchEntity patchEntity = new PatchEntity();
                            if (file.has("filename"))
                                patchEntity.setFileName(file.get("filename").asText());
                            if (file.has("contents_url"))
                                patchEntity.setContentsUrl(file.get("contents_url").asText());
                            if (file.has("patch")) {
                                String patch = file.get("patch").asText();
                                patchEntity.setPatch(patch);
                                commitRepository.updateLength(oid,patch.length());
                            }
                            patchEntity.setCommitOid(oid);
                            if (patchEntity.getPatch() != null && patchRepository.findByPatch(patchEntity.getPatch()) == null) {
                                Mono<Void> saveAndSendMono = savePatchEntity(patchEntity)
                                        .flatMap(savedEntity -> messageSenderService.CommitSendMessage(commitEntity))
                                        .then();
                                monos.add(saveAndSendMono);
                            }
                        }
                    }
                    return Flux.merge(monos);
                })
                .then();
    }
    private Mono<PatchEntity> savePatchEntity(PatchEntity patchEntity) {
        return Mono.fromCallable(() -> patchRepository.save(patchEntity));
    }
}