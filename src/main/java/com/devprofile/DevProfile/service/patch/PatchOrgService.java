package com.devprofile.DevProfile.service.patch;

import com.devprofile.DevProfile.entity.PatchEntity;
import com.devprofile.DevProfile.repository.PatchRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PatchOrgService {

    private final PatchRepository patchRepository;
    private final WebClient webClient;

    public PatchOrgService(PatchRepository patchRepository, @Qualifier("patchWebClient") WebClient webClient) {
        this.patchRepository = patchRepository;
        this.webClient = webClient;
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
                                                    .flatMapIterable(response -> response.get("files"))
                                                    .map(file -> {
                                                        PatchEntity patchEntity = new PatchEntity();
                                                        if (file.has("filename"))
                                                            patchEntity.setFileName(file.get("filename").asText());
                                                        if (file.has("raw_url"))
                                                            patchEntity.setRawUrl(file.get("raw_url").asText());
                                                        if (file.has("contents_url"))
                                                            patchEntity.setContentsUrl(file.get("contents_url").asText());
                                                        if (file.has("patch"))
                                                            patchEntity.setPatch(file.get("patch").asText());
                                                        patchEntity.setCommitOid(oid);
                                                        return patchEntity;
                                                    });
                                        });
                            });
                })
                .flatMap(patchEntity -> Mono.fromCallable(() -> patchRepository.save(patchEntity)))
                .doOnError(e -> log.error("Error saving patch: ", e))
                .onErrorContinue((throwable, o) -> log.error("Error continuing with next patch", throwable))
                .then();
    }
}