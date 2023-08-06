package com.devprofile.DevProfile.service.graphql;

import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.service.commit.CommitOrgService;
import com.devprofile.DevProfile.service.commit.ContributorsOrgService;
import com.devprofile.DevProfile.service.patch.PatchOrgService;
import com.devprofile.DevProfile.service.repository.LanguageService;
import com.devprofile.DevProfile.service.repository.OrgRepoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphOrgService {


    private final GraphQLService graphQLService;
    private final OrgRepoService orgRepoService;
    private final CommitOrgService commitOrgService;
    private final PatchOrgService patchOrgService;
    private final LanguageService languageService;
    private final ContributorsOrgService contributorsOrgService;


    private Mono<Map<String, Map<String, List<String>>>> fetchOrganizationRepoCommits(JsonNode organizations) {
        Map<String, Map<String, List<String>>> orgRepoCommits = new HashMap<>();
        if (organizations != null) {
            organizations.forEach(org -> {
                String orgName = org.get("name").asText();
                Map<String, List<String>> repoCommits = new HashMap<>();
                if (org.has("repositories") && org.get("repositories").has("nodes")) {
                    org.get("repositories").get("nodes").forEach(repoNode -> {
                        String repoName = repoNode.get("name").asText();
                        List<String> oids = new ArrayList<>();
                        if (repoNode.has("defaultBranchRef") && repoNode.get("defaultBranchRef").has("target")
                                && repoNode.get("defaultBranchRef").get("target").has("history")
                                && repoNode.get("defaultBranchRef").get("target").get("history").has("edges")) {
                            JsonNode history = repoNode.get("defaultBranchRef").get("target").get("history");
                            history.get("edges").forEach(edge -> oids.add(edge.get("node").get("oid").asText()));
                        }
                        if (!oids.isEmpty()) {
                            repoCommits.put(repoName, oids);
                        }
                    });
                }
                if (!repoCommits.isEmpty()) {
                    orgRepoCommits.put(orgName, repoCommits);
                }
            });
        }
        return Mono.just(orgRepoCommits);
    }

    @Transactional
    public Mono<Void> orgOwnedRepositories(UserEntity user) {
        return getQueryTemplate(user)
                .flatMap(queryTemplate -> executeGraphQLRequest(user, queryTemplate))
                .flatMap(response -> {
                    JsonNode organizations = response.get("data").get("user").get("organizations").get("nodes");
                    String userName = user.getLogin();
                    String accessToken = user.getGitHubToken();
                    return saveRepositories(organizations, user.getId(), userName)
                            .then(saveCommits(organizations, user))
                            .flatMap(orgRepoCommits -> Flux.merge(
                                            countCommits(orgRepoCommits, userName, accessToken),
                                            savePatchs(orgRepoCommits, accessToken),
                                            orgLanguages(orgRepoCommits, accessToken, userName))
                                    .then());
                });
    }

    private Mono<String> getQueryTemplate(UserEntity user) {
        try {
            return Mono.just(graphQLService.getGraphQLQuery("org_repositories_query_graphqls"));
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
    public Mono<Void> saveRepositories(JsonNode organizations, Integer userId, String userName) {
        return orgRepoService.saveRepositories(organizations, userId, userName)
                .doOnSuccess(aVoid -> log.info("Repositories saved successfully for user with id {}", userId))
                .doOnError(e -> log.error("Error while saving repositories for user with id {}", userId, e));
    }

    @Transactional
    public Mono<Map<String, Map<String, List<String>>>> saveCommits(JsonNode organizations, UserEntity user) {
        String userName = user.getLogin();
        Integer userId = user.getId();

        return commitOrgService.saveCommits(organizations, userName, userId)
                .doOnSuccess(aVoid -> log.info("Commits saved successfully for organization"))
                .doOnError(e -> log.error("Error while saving commits for organization", e))
                .flatMap(aVoid -> commitOrgService.updateDates().thenReturn(aVoid))
                .then(fetchOrganizationRepoCommits(organizations));
    }

    @Transactional
    public Mono<Void> countCommits(Map<String, Map<String, List<String>>> orgRepoCommits, String userName, String accessToken) {
        return contributorsOrgService.countCommits(orgRepoCommits, userName, accessToken)
                .doOnSuccess(aVoid -> log.info("Commits counted successfully for organization"))
                .doOnError(e -> log.error("Error while counting commits for organization", e));
    }

    @Transactional
    public Mono<Void> savePatchs(Map<String, Map<String, List<String>>> orgRepoCommits, String accessToken) {
        return patchOrgService.savePatchs(accessToken, orgRepoCommits)
                .doOnSuccess(aVoid -> log.info("Patches saved successfully for organization"))
                .doOnError(e -> log.error("Error while saving patches for organization", e));
    }

    @Transactional
    public Mono<Void> orgLanguages(Map<String, Map<String, List<String>>> orgRepoCommits, String accessToken, String userName) {
        return languageService.orgLanguages(orgRepoCommits, accessToken, userName)
                .doOnSuccess(aVoid -> log.info("Languages for organization processed successfully"))
                .doOnError(e -> log.error("Error while processing languages for organization", e));
    }
}