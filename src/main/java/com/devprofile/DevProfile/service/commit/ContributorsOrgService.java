package com.devprofile.DevProfile.service.commit;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ContributorsOrgService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final GitRepository gitRepository;

    public ContributorsOrgService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper, GitRepository gitRepository) {
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
        this.objectMapper = objectMapper;
        this.gitRepository = gitRepository;
    }

    public void countCommits(Map<String, Map<String, List<String>>> orgRepoCommits,String userName,String token) {
        orgRepoCommits.forEach((orgName, repoCommitMap) -> {
            repoCommitMap.forEach((repoName, commitList) -> {
                webClient.get()
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

                                RepositoryEntity savedEntity = gitRepository.save(repositoryEntity);
                                return Mono.just(savedEntity);
                            } catch (Exception e) {
                                return Mono.error(new RuntimeException("Fail", e));
                            }
                        }).subscribe();
            });
        });
    }
}