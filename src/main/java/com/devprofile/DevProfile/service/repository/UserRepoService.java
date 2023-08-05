package com.devprofile.DevProfile.service.repository;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
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
public class UserRepoService extends AbstractRepositoryService {

    private final MessageSenderService messageSenderService;

    public UserRepoService(GitRepository gitRepository, WebClient.Builder webClientBuilder, MessageSenderService messageSenderService) {
        super(gitRepository, webClientBuilder);
        this.messageSenderService = messageSenderService;
    }



    public Mono<Void> saveRepositories(JsonNode repos, Integer userId) {
        return Flux.fromIterable(repos)
                .flatMap(repo -> this.saveRepository(repo, userId))
                .doOnNext(savedRepo -> {
                    messageSenderService.RepoSendMessage(savedRepo)
                            .subscribe(result -> log.info("Sent message: " + result));
                })
                .then();
    }


    public Mono<RepositoryEntity> saveRepository(JsonNode repo, Integer userId) {
        return Mono.fromCallable(() -> {
            String repoNodeId = repo.get("id").asText();
            Optional<RepositoryEntity> existingRepoEntity = gitRepository.findByRepoNodeId(repoNodeId);

            RepositoryEntity repositoryEntity = existingRepoEntity.orElseGet(RepositoryEntity::new);

            if (!existingRepoEntity.isPresent()) {
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
            }

            RepositoryEntity savedEntity = gitRepository.save(repositoryEntity);
            return savedEntity;
        });
    }

    public void saveRepository(RepositoryEntity repositoryEntity) {
    }
}