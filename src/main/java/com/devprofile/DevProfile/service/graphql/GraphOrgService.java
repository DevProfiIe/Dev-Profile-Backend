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
    public Mono<Void> orgOwnedRepositories(UserEntity user) {
        return Mono.defer(() -> {
            Integer userId = user.getId();
            String userNodeId = user.getNode_id();
            String userName = user.getLogin();
            String accessToken = user.getGitHubToken();

            String queryTemplate = null;
            try {
                queryTemplate = graphQLService.getGraphQLQuery("org_repositories_query_graphqls");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("login", userName);
            variables.put("node_id", userNodeId);

            GraphQLService.CustomGraphQLRequest request = new GraphQLService.CustomGraphQLRequest(queryTemplate, variables);

            try {
                    return graphQLService.sendGraphQLRequest(user, request)
                            .flatMap(response -> {
                                if (response.has("errors")) {
                                    System.out.println("GraphQL Errors: " + response.get("errors"));
                                }
                                JsonNode organizations = response.get("data").get("user").get("organizations").get("nodes");
                                orgRepoService.saveRepositories(organizations, userId, userName);
                                commitOrgService.saveCommits(organizations, userName, userId);
                                commitOrgService.updateDates();

                                return fetchOrganizationRepoCommits(organizations)
                                        .flatMap(orgRepoCommits -> {
                                            contributorsOrgService.countCommits(orgRepoCommits, userName, accessToken);
                                            patchOrgService.savePatchs(accessToken, orgRepoCommits); // Call the synchronous method without chaining
                                            return languageService.orgLanguages(orgRepoCommits, accessToken, userName);
                                        })
                                        .then();
                            });
            }catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }