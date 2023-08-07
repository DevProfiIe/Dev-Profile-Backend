package com.devprofile.DevProfile.service.repository;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import com.devprofile.DevProfile.service.commit.CommitOrgService;
import com.devprofile.DevProfile.service.rabbitmq.MessageOrgSenderService;
import com.devprofile.DevProfile.service.rabbitmq.MessageSenderService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@Slf4j
public class OrgRepoService extends AbstractRepositoryService {
    private final CommitOrgService commitOrgService;
    private final MessageOrgSenderService messageOrgSenderService;

    public OrgRepoService(GitRepository gitRepository, WebClient.Builder webClientBuilder, CommitOrgService commitOrgService, MessageSenderService messageSenderService, MessageOrgSenderService messageOrgSenderService) {
        super(gitRepository, webClientBuilder);
        this.commitOrgService = commitOrgService;
        this.messageOrgSenderService = messageOrgSenderService;
    }

    public Mono<Void> saveRepositories(JsonNode orgs, Integer userId, String userName) {
        return Flux.fromIterable(orgs)
                .flatMap((JsonNode org) -> Flux.fromIterable(org.get("repositories").get("nodes"))
                        .flatMap(repo -> this.saveRepository(repo, userId, org.get("name").asText(),userName))
                        .doOnNext(savedRepo -> {
                            messageOrgSenderService.orgRepoSendMessage(savedRepo)
                                    .subscribe(result -> log.info("Sent message: " + result),
                                            error -> log.error("Error while sending message for repository: ", error));
                        })
                )
                .then()
                .doOnSuccess(aVoid -> log.info("Repositories saved successfully for user with id {}", userId))
                .doOnError(e -> log.error("Error while saving repositories for user with id {}", userId, e));
    }


    private Mono<RepositoryEntity> saveRepository(JsonNode repo, Integer userId, String orgName,String userName) {
        return Mono.fromCallable(() -> {
            String repoNodeId = repo.get("id").asText();
            Optional<RepositoryEntity> existingRepoEntity = gitRepository.findByRepoNodeId(repoNodeId);

            RepositoryEntity repositoryEntity = existingRepoEntity.orElseGet(RepositoryEntity::new);

            if (!existingRepoEntity.isPresent()) {
                JsonNode defaultBranchRef = repo.get("defaultBranchRef");
                if (defaultBranchRef != null) {
                    JsonNode target = defaultBranchRef.get("target");
                    if (target != null) {
                        JsonNode history = target.get("history");
                        if (history != null) {
                            JsonNode edges = history.get("edges");
                            if (edges != null && !edges.isNull() && edges.elements().hasNext()) {
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
                                repositoryEntity.setUserName(userName);

                                gitRepository.save(repositoryEntity);
                            }
                        }
                    }
                }
            }
            return repositoryEntity;
        });
    }
}