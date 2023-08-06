package com.devprofile.DevProfile.service.repository;

import com.devprofile.DevProfile.entity.LanguageDuration;
import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import com.devprofile.DevProfile.service.rabbitmq.MessageOrgSenderService;
import com.devprofile.DevProfile.service.rabbitmq.MessageSenderService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class LanguageService {

    private GitRepository gitRepository;
    private WebClient webClient;
    private MessageOrgSenderService messageOrgSenderService;
    private MessageSenderService messageSenderService;

    public LanguageService(GitRepository gitRepository, WebClient.Builder webClientBuilder, MessageOrgSenderService messageOrgSenderService,MessageSenderService messageSenderService) {
        this.gitRepository = gitRepository;
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
        this.messageOrgSenderService = messageOrgSenderService;
        this.messageSenderService = messageSenderService;

    }


    public Mono<Void> repoLanguages(Map<String, List<String>> repoOidsMap, String userName, String token) {
        List<Mono<Void>> languageMonos = new ArrayList<>();

        for (String repoName : repoOidsMap.keySet()) {
            String url = String.format("/repos/%s/%s/languages", userName, repoName);

            Mono<Void> languageMono = webClient.get().uri(url)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .onErrorResume(e -> {
                        log.error("Error while retrieving language data for repository " + repoName, e);
                        return Mono.empty();
                    })
                    .flatMap(response -> {
                        RepositoryEntity repoEntity = gitRepository.findByRepoName(repoName).orElse(new RepositoryEntity());
                        LocalDateTime startDate = repoEntity.getStartDate();
                        LocalDateTime endDate = repoEntity.getEndDate();

                        if (startDate != null && endDate != null) {
                            int totalDays = Period.between(startDate.toLocalDate(), endDate.toLocalDate()).getDays();

                            List<String> languages = new ArrayList<>();
                            response.fieldNames().forEachRemaining(languages::add);

                            List<LanguageDuration> languageDurations = languages.stream()
                                    .map(language -> new LanguageDuration(language, totalDays, userName))
                                    .collect(Collectors.toList());

                            List<LanguageDuration> mergedLanguageDurations = Stream.concat(
                                    repoEntity.getLanguageDurations().stream(),
                                    languageDurations.stream()
                            ).distinct().collect(Collectors.toList());

                            repoEntity.setLanguageDurations(mergedLanguageDurations);
                            return Mono.fromRunnable(() -> {
                                gitRepository.save(repoEntity);
                                messageSenderService.RepoSendMessage(repoEntity).subscribe();
                            }).then();
                        } else {
                            log.warn("Start date or end date is null for repository: " + repoName);
                            return Mono.empty();
                        }
                    });

            languageMonos.add(languageMono);
        }

        return Flux.concat(languageMonos).then();
    }


    public Mono<Void> orgLanguages(Map<String, Map<String, List<String>>> orgRepoCommits, String token, String userName) {
        List<Mono<Void>> languageMonos = new ArrayList<>();

        orgRepoCommits.forEach((orgName, repoCommits) -> {
            repoCommits.keySet().forEach(repoName -> {
                String url = String.format("/repos/%s/%s/languages", orgName, repoName);

                Mono<Void> languageMono = webClient.get().uri(url)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .onErrorResume(e -> {
                            log.error("Error while retrieving language data for repository " + repoName, e);
                            return Mono.empty();
                        })
                        .flatMap(response -> {
                            RepositoryEntity repoEntity = gitRepository.findByRepoName(repoName).orElse(new RepositoryEntity());
                            LocalDateTime startDate = repoEntity.getStartDate();
                            LocalDateTime endDate = repoEntity.getEndDate();

                            if (startDate != null && endDate != null) {
                                int totalDays = Period.between(startDate.toLocalDate(), endDate.toLocalDate()).getDays();

                                List<String> languages = new ArrayList<>();
                                response.fieldNames().forEachRemaining(languages::add);

                                List<LanguageDuration> languageDurations = languages.stream()
                                        .map(language -> new LanguageDuration(language, totalDays, userName))
                                        .collect(Collectors.toList());

                                repoEntity.setLanguageDurations(languageDurations);
                                return Mono.fromRunnable(() -> {
                                    gitRepository.save(repoEntity);
                                    messageOrgSenderService.orgRepoSendMessage(repoEntity).subscribe();
                                }).then();
                            } else {
                                log.warn("Start date or end date is null for repository: " + repoName);
                                return Mono.empty();
                            }
                        });

                languageMonos.add(languageMono);
            });
        });

        return Flux.concat(languageMonos).then();
    }
}