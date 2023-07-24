package com.devprofile.DevProfile.service.graphql;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.service.commit.CommitOrgService;
import com.devprofile.DevProfile.service.commit.CommitUserService;
import com.devprofile.DevProfile.service.commit.ContributorsUserService;
import com.devprofile.DevProfile.service.patch.PatchUserService;
import com.devprofile.DevProfile.service.repository.LanguageService;
import com.devprofile.DevProfile.service.repository.UserRepoService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    private final UserRepoService ownRepoService;

    private final GraphQLService graphQLService;

    private final CommitUserService commitUserService;
    private final PatchUserService patchUserService;
    private final LanguageService languageService;
    private final ContributorsUserService contributorsUserService;


    public Mono<Void> userOwnedRepositories(UserEntity user) throws IOException, IOException {
        Integer userId = user.getId();
        String userNodeId = user.getNode_id();
        String userName = user.getLogin();
        String accessToken = user.getGitHubToken();

        String queryTemplate = graphQLService.getGraphQLQuery("own_repositories_query_graphqls");

        Map<String, Object> variables = new HashMap<>();
        variables.put("login", userName);
        variables.put("node_id", userNodeId);

        GraphQLService.CustomGraphQLRequest request = new GraphQLService.CustomGraphQLRequest(queryTemplate, variables);

        return graphQLService.sendGraphQLRequest(user, request)
                .doOnNext(response -> {
                    if (response.has("errors")) {
                        System.out.println("GraphQL Errors: " + response.get("errors"));
                    }

                    JsonNode repositories = response.get("data").get("user").get("repositories").get("nodes");
                    ownRepoService.saveRepositories(repositories, userId);
                    Map<String, List<String>> repoOidsMap = commitUserService.saveCommits(repositories,userName,userId);
                    commitUserService.updateDates();
                    contributorsUserService.countCommits(repoOidsMap,userName,accessToken);
                    patchUserService.savePatchs(userName,accessToken,repoOidsMap);
                    languageService.repoLanguages(repoOidsMap,userName,accessToken);

                }).then();
    }
}