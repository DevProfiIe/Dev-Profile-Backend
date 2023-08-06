package com.devprofile.DevProfile.service.graphql;

import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.service.commit.CommitUserService;
import com.devprofile.DevProfile.service.commit.ContributorsUserService;
import com.devprofile.DevProfile.service.patch.PatchUserService;
import com.devprofile.DevProfile.service.repository.LanguageService;
import com.devprofile.DevProfile.service.repository.UserRepoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphUserService {

    private final UserRepoService userRepoService;
    private final GraphQLService graphQLService;
    private final CommitUserService commitUserService;
    private final PatchUserService patchUserService;
    private final LanguageService languageService;
    private final ContributorsUserService contributorsUserService;



    @Transactional
    public Mono<Void> userOwnedRepositories(UserEntity user) {
        return getQueryTemplate(user)
                .flatMap(queryTemplate -> executeGraphQLRequest(user, queryTemplate))
                .flatMap(response -> {
                    JsonNode repositories = response.get("data").get("user").get("repositories").get("nodes");
                    String userName = user.getLogin();
                    String accessToken = user.getGitHubToken();
                    return saveRepositories(repositories, user.getId())
                            .then(saveCommits(repositories, user))
                            .flatMap(repoOidsMap -> Flux.merge(
                                            countCommits(repoOidsMap, userName, accessToken),
                                            savePatchs(repoOidsMap, user),
                                            repoLanguages(repoOidsMap, user))
                                    .then())
                            .then();
                });
    }

    private Mono<String> getQueryTemplate(UserEntity user) {
        try {
            return Mono.just(graphQLService.getGraphQLQuery("own_repositories_query_graphqls"));
        } catch (IOException e) {
            return Mono.error(new RuntimeException(e));
        }
    }

    private Mono<JsonNode> executeGraphQLRequest(UserEntity user, String queryTemplate) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("login", user.getLogin());
        variables.put("node_id", user.getNode_id());
        GraphQLService.CustomGraphQLRequest request = new GraphQLService.CustomGraphQLRequest(queryTemplate, variables);

        try {
            return graphQLService.sendGraphQLRequest(user, request);
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException(e));
        }
    }


    @Transactional
    public Mono<Void> saveRepositories(JsonNode repositories, Integer userId) {
        return userRepoService.saveRepositories(repositories, userId)
                .doOnSuccess(aVoid -> log.info("Repositories saved successfully for user with id {}", userId))
                .doOnError(e -> log.error("Error while saving repositories for user with id {}", userId, e));
    }


    @Transactional
    public Mono<Map<String, List<String>>> saveCommits(JsonNode repositories, UserEntity user) {
        return commitUserService.saveCommits(repositories, user.getLogin(), user.getId())
                .doOnSuccess(repoOidsMap -> log.info("Commits saved successfully for user {}", user.getLogin()))
                .doOnError(e -> log.error("Error while saving commits for user {}", user.getLogin(), e))
                .flatMap(repoOidsMap -> commitUserService.updateDates()
                        .thenReturn(repoOidsMap));
    }

    @Transactional
    public Mono<Void> countCommits(Map<String, List<String>> repoOidsMap, String userName, String accessToken) {
        return contributorsUserService.countCommits(repoOidsMap, userName, accessToken)
                .doOnSuccess(aVoid -> log.info("Commits counted successfully for user {}", userName))
                .doOnError(e -> log.error("Error while counting commits for user {}", userName, e));
    }

    @Transactional
    public Mono<Void> savePatchs(Map<String, List<String>> repoOidsMap, UserEntity user) {
        return patchUserService.savePatchs(user.getLogin(), user.getGitHubToken(), repoOidsMap)
                .doOnSuccess(aVoid -> log.info("Patches saved successfully for user {}", user.getLogin()))
                .doOnError(e -> log.error("Error while saving patches for user {}", user.getLogin(), e));
    }



    @Transactional
    public Mono<Void> repoLanguages(Map<String, List<String>> repoOidsMap, UserEntity user) {
        return languageService.repoLanguages(repoOidsMap, user.getLogin(), user.getGitHubToken())
                .doOnSuccess(aVoid -> log.info("Languages for user {} processed successfully", user.getLogin()))
                .doOnError(e -> log.error("Error while processing languages for user {}", user.getLogin(), e));
    }

}
