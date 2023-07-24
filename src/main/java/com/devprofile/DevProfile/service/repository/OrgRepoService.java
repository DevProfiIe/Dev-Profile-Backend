package com.devprofile.DevProfile.service.repository;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import com.devprofile.DevProfile.service.commit.CommitOrgService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OrgRepoService extends AbstractRepositoryService {
    private final CommitOrgService commitOrgService;

    public OrgRepoService(GitRepository gitRepository, WebClient.Builder webClientBuilder, CommitOrgService commitOrgService) {
        super(gitRepository, webClientBuilder);
        this.commitOrgService = commitOrgService;
    }

    public Mono<Void> saveRepositories(JsonNode orgs, Integer userId, String userName) {
        return Flux.fromIterable(orgs)
                .flatMap(org -> {
                    String orgName = org.get("name").asText();
                    return Flux.fromIterable(org.get("repositories").get("nodes"))
                            .flatMap(repo -> this.saveRepository(repo, userId, orgName));
                })
                .then()
                .onErrorResume(e -> {
                    log.error("Error saving repositories: ", e);
                    return Mono.empty();
                });
    }

    private Mono<RepositoryEntity> saveRepository(JsonNode repo, Integer userId, String orgName) {
        RepositoryEntity repositoryEntity = new RepositoryEntity();

        JsonNode defaultBranchRef = repo.get("defaultBranchRef");
        if (defaultBranchRef != null) {
            JsonNode target = defaultBranchRef.get("target");
            if (target != null) {
                JsonNode history = target.get("history");
                if (history != null) {
                    JsonNode edges = history.get("edges");
                    if (edges == null || edges.isNull() || !edges.elements().hasNext()) {
                        return Mono.empty();
                    }
                }
            }
        }

        String repoNodeId = repo.get("id").asText();
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
        return Mono.empty();
    }
}