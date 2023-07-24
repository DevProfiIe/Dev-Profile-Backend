package com.devprofile.DevProfile.service.repository;


import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class LanguageService {

    private GitRepository gitRepository;
    private WebClient webClient;

    public LanguageService(GitRepository gitRepository, WebClient.Builder webClientBuilder) {
        this.gitRepository = gitRepository;
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
    }

    public void repoLanguages(Map<String, List<String>> repoOidsMap, String userName,String token) {
        repoOidsMap.keySet().forEach(repoName -> {

            String url = String.format("/repos/%s/%s/languages", userName, repoName);

            Mono<JsonNode> responseMono = webClient.get().uri(url)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(JsonNode.class);

            responseMono.subscribe(response -> {
                Set<String> languages = new HashSet<>();
                response.fieldNames().forEachRemaining(languages::add);

                RepositoryEntity repoEntity = gitRepository.findByRepoName(repoName).orElse(new RepositoryEntity());
                repoEntity.setRepoLanguages(languages);

//                orgLanguages(repoOidsMap,token);
                gitRepository.save(repoEntity);
            });
        });
    }


    public void orgLanguages(Map<String, List<String>> orgsRepoNamesMap, String token) {
        orgsRepoNamesMap.forEach((orgName, repoNamesList) -> {
            repoNamesList.forEach(repoName -> {
                String url = String.format("/repos/%s/%s/languages", orgName, repoName);
                Mono<JsonNode> responseMono = webClient.get().uri(url)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .bodyToMono(JsonNode.class);

                responseMono.subscribe(response -> {
                    Set<String> languages = new HashSet<>();
                    response.fieldNames().forEachRemaining(languages::add);

                    RepositoryEntity repoEntity = gitRepository.findByRepoName(repoName)
                            .orElse(new RepositoryEntity());
                    repoEntity.setRepoLanguages(languages);

                    gitRepository.save(repoEntity);
                });
            });
        });
    }
}
