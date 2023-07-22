package com.devprofile.DevProfile.service.commit;

import com.devprofile.DevProfile.entity.PatchEntity;
import com.devprofile.DevProfile.repository.PatchRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatchService {

    private final PatchRepository patchRepository;

    private WebClient webClient;


    public void extractAndSavePatchs(String userName, String accessToken, Map<String, List<String>> repoOidsMap) {


        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) //10MB로 설정
                .build();

        this.webClient = WebClient.builder()
                .exchangeStrategies(exchangeStrategies)
                .build();

        for (Map.Entry<String, List<String>> entry : repoOidsMap.entrySet()) {
            String repoName = entry.getKey();
            List<String> oids = entry.getValue();

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
                                            patchEntity.setCommitSha(oid);
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
    }

    @Transactional
    public void savePatchInfo(List<PatchEntity> patches) {
        patchRepository.saveAll(patches);
        patchRepository.flush();
    }
}
