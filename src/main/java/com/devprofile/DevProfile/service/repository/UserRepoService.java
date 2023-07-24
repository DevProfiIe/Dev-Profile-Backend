package com.devprofile.DevProfile.service.repository;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class UserRepoService extends AbstractRepositoryService {

    public UserRepoService(GitRepository gitRepository, WebClient.Builder webClientBuilder) {
        super(gitRepository, webClientBuilder);
    }

    public CompletableFuture<Void> saveRepositories(JsonNode repos, Integer userId) {
        List<CompletableFuture<RepositoryEntity>> futures = new ArrayList<>();

        repos.forEach(repo -> {
            futures.add(this.saveRepository(repo, userId));
        });

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[futures.size()])
        );

        return allFutures.exceptionally(e -> {
            log.error("Error saving repositories: ", e);
            return null;
        });
    }

    private CompletableFuture<RepositoryEntity> saveRepository(JsonNode repo,Integer userId) {
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

        return CompletableFuture.completedFuture(gitRepository.save(repositoryEntity));
    }
}
