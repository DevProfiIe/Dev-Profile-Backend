package com.devprofile.DevProfile.service.commit;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import com.devprofile.DevProfile.service.rabbitmq.MessageSenderService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ContributorsUserService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final GitRepository gitRepository;
    private final MessageSenderService messageSenderService;
    public ContributorsUserService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper, GitRepository gitRepository,MessageSenderService messageSenderService) {
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
        this.objectMapper = objectMapper;
        this.gitRepository = gitRepository;
        this.messageSenderService = messageSenderService;
    }


    public Mono<Void> countCommits(Map<String, List<String>> repoOidsMap, String userName, String token,Integer userId) {
        return Flux.fromIterable(repoOidsMap.entrySet())
                .flatMap(entry -> {
                    String repoName = entry.getKey();
                    return webClient.get()
                            .uri("/repos/{userName}/{repoName}/contributors", userName, repoName)
                            .header("Authorization", "Bearer " + token)
                            .retrieve()
                            .bodyToMono(String.class)
                            .onErrorResume(e -> {
                                if (e instanceof WebClientResponseException.NotFound) {
                                    log.error("404 error: Resource not found", e);
                                    return Mono.empty();
                                } else {
                                    log.error("Error calling API", e);
                                    return Mono.error(e);
                                }
                            })
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
                                    RepositoryEntity repositoryEntity = gitRepository.findByUserIdAndRepoName(userId,repoName).orElse(new RepositoryEntity());

                                    repositoryEntity.setTotalCommitCnt(totalCommitCnt);
                                    repositoryEntity.setMyCommitCnt(myCommitCnt);
                                    repositoryEntity.setTotalContributors(totalContributors);
                                    repositoryEntity.setRepoName(repoName);

                                    return Mono.fromCallable(() -> gitRepository.save(repositoryEntity))
                                            .flatMap(repoEntity -> messageSenderService.RepoSendMessage(repoEntity));
                                } catch (Exception e) {
                                    return Mono.error(new RuntimeException("Fail", e));
                                }
                            });
                })
                .then();
    }
}
