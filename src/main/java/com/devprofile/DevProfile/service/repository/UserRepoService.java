package com.devprofile.DevProfile.service.repository;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class UserRepoService extends AbstractRepositoryService {

    public UserRepoService(GitRepository gitRepository, WebClient.Builder webClientBuilder) {
        super(gitRepository, webClientBuilder);
    }

    public Mono<Void> saveRepositories(JsonNode repos, Integer userId) {
        return Flux.fromIterable(repos)
                .flatMap(repo -> this.saveRepository(repo, userId))
                .then()
                .onErrorResume(e -> {
                    log.error("Error saving repositories: ", e);
                    return Mono.empty();
                });
    }

    private Mono<RepositoryEntity> saveRepository(JsonNode repo,Integer userId) {
        RepositoryEntity repositoryEntity = new RepositoryEntity();

        String repoNodeId = repo.get("id").asText();
        String repoName = repo.get("name").asText();
        String repoCreated = repo.get("createdAt").asText();
        String repoUpdated = repo.get("updatedAt").asText();
        String repoUrl = repo.get("url").asText();
        String repoDesc = repo.get("description").asText();

        repositoryEntity.setRepoNodeId(repoNodeId);
        repositoryEntity.setRepoName(repoName);
        repositoryEntity.setRepoCreated(repoCreated);
        repositoryEntity.setRepoUpdated(repoUpdated);
        repositoryEntity.setRepoUrl(repoUrl);
        repositoryEntity.setRepoDesc(repoDesc);
        repositoryEntity.setUserId(userId);

        return Mono.just(gitRepository.save(repositoryEntity));
    }
}
