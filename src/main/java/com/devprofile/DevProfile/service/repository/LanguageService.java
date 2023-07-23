package com.devprofile.DevProfile.service.repository;


import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

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

    public Mono<RepositoryEntity> repoLanguages(RepositoryEntity repositoryEntity, String userName, List<String> orgRepositories) {
        String repoName = repositoryEntity.getRepoName();
        String url = String.format("/repos/%s/%s/languages", userName, repoName);

        return webClient.get().uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    Set<String> languages = repositoryEntity.getRepoLanguages();
                    languages.addAll(response.keySet());
                    repositoryEntity.setRepoLanguages(languages);
                    System.out.println("repositoryEntity = " + repositoryEntity);
                    return Mono.just(gitRepository.save(repositoryEntity));
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Failed to fetch languages for repository: " + url, e);
                    return Mono.empty();
                });
    }

    public Mono<RepositoryEntity> orgLanguages(RepositoryEntity repositoryEntity, String orgName) {
        String repoName = repositoryEntity.getRepoName();
        String url = String.format("/repos/%s/%s/languages", orgName, repoName);

        return webClient.get().uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    Set<String> languages = repositoryEntity.getRepoLanguages();
                    languages.addAll(response.keySet());
                    repositoryEntity.setRepoLanguages(languages);
                    System.out.println("repositoryEntity = " + repositoryEntity);
                    return Mono.just(gitRepository.save(repositoryEntity));
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Failed to fetch languages for repository: " + url, e);
                    return Mono.empty();
                });
    }
}
