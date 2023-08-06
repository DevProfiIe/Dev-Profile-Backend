package com.devprofile.DevProfile.service.commit;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import com.devprofile.DevProfile.service.rabbitmq.MessageOrgSenderService;
import com.devprofile.DevProfile.service.rabbitmq.MessageSenderService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ContributorsOrgService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final GitRepository gitRepository;
    private final MessageOrgSenderService messageOrgSenderService;

    public ContributorsOrgService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper, GitRepository gitRepository, MessageSenderService messageSenderService, MessageOrgSenderService messageOrgSenderService) {
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
        this.objectMapper = objectMapper;
        this.gitRepository = gitRepository;
        this.messageOrgSenderService = messageOrgSenderService;
    }

    public Mono<Void> countCommits(Map<String, Map<String, List<String>>> orgRepoCommits,String userName,String token) {
        return Flux.fromIterable(orgRepoCommits.entrySet())
                .flatMap(orgEntry -> {
                    String orgName = orgEntry.getKey();
                    Map<String, List<String>> repoCommitMap = orgEntry.getValue();

                    return Flux.fromIterable(repoCommitMap.entrySet())
                            .flatMap(repoEntry -> {
                                String repoName = repoEntry.getKey();

                                return webClient.get()
                                        .uri("/repos/{orgName}/{repoName}/contributors", orgName, repoName)
                                        .header("Authorization", "Bearer " + token)
                                        .retrieve()
                                        .bodyToMono(String.class)
                                        .flatMap(responseBody -> {
                                            try {
                                                List<Map<String, Object>> contributors = objectMapper.readValue(responseBody, new TypeReference<List<Map<String, Object>>>() {});
                                                int totalCommitCnt = 0;
                                                int myCommitCnt = 0;
                                                int totalContributors = contributors.size();

                                                for (Map<String, Object> contributor : contributors) {
                                                    int contribution = (int) contributor.get("contributions");
                                                    totalCommitCnt += contribution;
                                                    if (contributor.get("login").equals(userName)) {
                                                        myCommitCnt = contribution;
                                                    }
                                                }
                                                RepositoryEntity repositoryEntity = gitRepository.findByRepoName(repoName).orElse(new RepositoryEntity());

                                                repositoryEntity.setTotalCommitCnt(totalCommitCnt);
                                                repositoryEntity.setMyCommitCnt(myCommitCnt);
                                                repositoryEntity.setTotalContributors(totalContributors);
                                                repositoryEntity.setRepoName(repoName);

                                                return Mono.fromCallable(() -> gitRepository.save(repositoryEntity))
                                                        .flatMap(repoEntity -> messageOrgSenderService.orgRepoSendMessage(repoEntity));
                                            } catch (Exception e) {
                                                return Mono.error(new RuntimeException("Fail", e));
                                            }
                                        });
                            });
                }).then();
    }
}