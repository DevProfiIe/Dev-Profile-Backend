package com.devprofile.DevProfile.service.repository;

import com.devprofile.DevProfile.entity.LanguageDuration;
import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Slf4j
@Service
public class LanguageService {

    private GitRepository gitRepository;
    private WebClient webClient;

    public LanguageService(GitRepository gitRepository, WebClient.Builder webClientBuilder) {
        this.gitRepository = gitRepository;
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
    }

    public void repoLanguages(Map<String, List<String>> repoOidsMap, String userName, String token) {
        repoOidsMap.keySet().forEach(repoName -> {

            String url = String.format("/repos/%s/%s/languages", userName, repoName);

            Mono<JsonNode> responseMono = webClient.get().uri(url)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(JsonNode.class);

            responseMono.subscribe(response -> {
                List<LanguageDuration> languageDurations = new ArrayList<>();
                RepositoryEntity repoEntity = gitRepository.findByRepoName(repoName).orElse(new RepositoryEntity());

                LocalDate startDate = repoEntity.getStartDate();
                LocalDate endDate = repoEntity.getEndDate();

                Integer totalDays = Period.between(startDate, endDate).getDays();

                response.fieldNames().forEachRemaining(language -> {
                    languageDurations.add(new LanguageDuration(language, totalDays, userName));
                });


                repoEntity.setLanguageDurations(languageDurations);

                gitRepository.save(repoEntity);
            });
        });
    }

    public Mono<Void> orgLanguages(Map<String, Map<String, List<String>>> orgRepoCommits, String token,String userName) {
        return Mono.fromRunnable(() -> {
            orgRepoCommits.forEach((orgName, repoCommits) -> {
                repoCommits.keySet().forEach(repoName -> {
                    String url = String.format("/repos/%s/%s/languages", orgName, repoName);

                    Mono<JsonNode> responseMono = webClient.get().uri(url)
                            .header("Authorization", "Bearer " + token)
                            .retrieve()
                            .bodyToMono(JsonNode.class);

                    responseMono.subscribe(response -> {
                        List<LanguageDuration> languageDurations = new ArrayList<>();
                        RepositoryEntity repoEntity = gitRepository.findByRepoName(repoName).orElse(new RepositoryEntity());

                        LocalDate startDate = repoEntity.getStartDate();
                        LocalDate endDate = repoEntity.getEndDate();

                        Integer totalDays = Period.between(startDate, endDate).getDays();

                        response.fieldNames().forEachRemaining(language -> {
                            languageDurations.add(new LanguageDuration(language, totalDays, userName));
                        });

                        repoEntity.setLanguageDurations(languageDurations);

                        gitRepository.save(repoEntity);
                    });
                });
            });
        });
    }
}
