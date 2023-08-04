package com.devprofile.DevProfile.service.patch;

import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.entity.PatchEntity;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.devprofile.DevProfile.repository.PatchRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PatchOrgService {

    private final PatchRepository patchRepository;
    private final WebClient webClient;
    private final CommitRepository commitRepository;

    public PatchOrgService(PatchRepository patchRepository, @Qualifier("patchWebClient") WebClient webClient, CommitRepository commitRepository) {
        this.patchRepository = patchRepository;
        this.webClient = webClient;
        this.commitRepository = commitRepository;
    }


    public void savePatchs(String accessToken, Map<String, Map<String, List<String>>> orgRepoCommits) {
        for (Map.Entry<String, Map<String, List<String>>> orgEntry : orgRepoCommits.entrySet()) {
            String orgName = orgEntry.getKey();
            Map<String, List<String>> repoCommits = orgEntry.getValue();
            for (Map.Entry<String, List<String>> repoEntry : repoCommits.entrySet()) {
                String repoName = repoEntry.getKey();
                List<String> oids = repoEntry.getValue();
                for (String oid : oids) {
                    String commitDetailUrl = "https://api.github.com/repos/" + orgName + "/" + repoName + "/commits/" + oid;
                    webClient.get()
                            .uri(commitDetailUrl)
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .flatMap(response -> processResponse(response, oid))
                            .subscribe();
                }
            }
        }
    }
    private Mono<Void> processResponse(JsonNode response, String oid) {
        CommitEntity commitEntity = commitRepository.findByCommitOid(oid).orElseThrow();
        for (JsonNode file : response.get("files")) {
            PatchEntity patchEntity = new PatchEntity();
            if (file.has("filename"))
                patchEntity.setFileName(file.get("filename").asText());
            if (file.has("raw_url"))
                patchEntity.setRawUrl(file.get("raw_url").asText());
            if (file.has("contents_url"))
                patchEntity.setContentsUrl(file.get("contents_url").asText());
            if (file.has("patch")) {
                String patch = file.get("patch").asText();
                patchEntity.setPatch(patch);
                commitRepository.updateLength(oid,patch.length());
            }
            patchEntity.setCommitOid(oid);
            if (patchEntity.getPatch() != null && patchRepository.findByPatch(patchEntity.getPatch()) == null) {
                savePatchEntity(patchEntity);
            }
        }
        return Mono.empty();
    }

    private void savePatchEntity(PatchEntity patchEntity) {
        patchRepository.save(patchEntity);
    }
}