package com.devprofile.DevProfile.service.repository;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import com.devprofile.DevProfile.service.commit.CommitOrgService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class OrgRepoService extends AbstractRepositoryService {
    private final CommitOrgService commitOrgService;

    public OrgRepoService(GitRepository gitRepository, WebClient.Builder webClientBuilder, CommitOrgService commitOrgService) {
        super(gitRepository, webClientBuilder);
        this.commitOrgService = commitOrgService;
    }

    public CompletableFuture<Void> saveRepositories(JsonNode orgs, Integer userId, String userName) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        orgs.forEach(org -> {
            String orgName = org.get("name").asText();
            org.get("repositories").get("nodes").forEach(repo -> {
                CompletableFuture<Void> result = this.saveRepository(repo, userId, orgName);
                if (result != null) {
                    futures.add(result);
                }
            });
        });

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[futures.size()])
        );

        return allFutures.exceptionally(e -> {
            log.error("Error saving repositories: ", e);
            return null;
        });
    }


    private CompletableFuture<Void> saveRepository(JsonNode repo, Integer userId, String orgName) {
        String repoNodeId = repo.get("id").asText();
        Optional<RepositoryEntity> existingRepoEntity = gitRepository.findByRepoNodeId(repoNodeId);

        if(!existingRepoEntity.isPresent()) {
            RepositoryEntity repositoryEntity = new RepositoryEntity();

            JsonNode defaultBranchRef = repo.get("defaultBranchRef");
            if (defaultBranchRef != null) {
                JsonNode target = defaultBranchRef.get("target");
                if (target != null) {
                    JsonNode history = target.get("history");
                    if (history != null) {
                        JsonNode edges = history.get("edges");
                        if (edges == null || edges.isNull() || !edges.elements().hasNext()) {
                            return null;
                        }
                    }
                }
            }

            String repoName = repo.get("name").asText();
            String repoDesc = repo.get("description").asText();
            String repoCreated = repo.get("createdAt").asText();
            String repoUpdated = repo.get("updatedAt").asText();
            String repoUrl = repo.get("url").asText();

            repositoryEntity.setOrgName(orgName);
            repositoryEntity.setRepoNodeId(repoNodeId);
            repositoryEntity.setRepoName(repoName);
            repositoryEntity.setRepoDesc(repoDesc);
            repositoryEntity.setRepoCreated(repoCreated);
            repositoryEntity.setRepoUpdated(repoUpdated);
            repositoryEntity.setRepoUrl(repoUrl);
            repositoryEntity.setUserId(userId);

            gitRepository.save(repositoryEntity);

        }
        return CompletableFuture.completedFuture(null);
    }
}