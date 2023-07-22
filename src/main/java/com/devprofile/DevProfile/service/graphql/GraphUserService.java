package com.devprofile.DevProfile.service.graphql;

import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.service.commit.CommitService;
import com.devprofile.DevProfile.service.commit.PatchService;
import com.devprofile.DevProfile.service.repository.RepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.io.IOException;
import java.util.HashMap;

import java.util.List;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphUserService {

    private final CommitService commitService;
    private final GraphQLService graphQLService;
    private final RepositoryService repositoryService;

    private final PatchService patchService;

    public Mono<Void> UserSaves(UserEntity user) throws IOException {
        Integer userId = user.getId();
        String userNodeId = user.getNode_id();
        String userName = user.getLogin();

        String accessToken = user.getGitHubToken();


        String queryTemplate = graphQLService.getGraphQLQuery("commits_query_graphqls");

        Map<String, Object> variables = new HashMap<>();
        variables.put("login", userName);
        variables.put("node_id", userNodeId);

        GraphQLService.CustomGraphQLRequest request = new GraphQLService.CustomGraphQLRequest(queryTemplate, variables);

        return graphQLService.sendGraphQLRequest(user, request)
                .doOnNext(response -> {
                    if (response.has("errors")) {
                        System.out.println("GraphQL Errors: " + response.get("errors"));
                    }

                    repositoryService.extractAndSaveRepositories(response,userId);
                    Map<String, List<String>> oids = commitService.extractAndSaveCommits(response, userId);

                    patchService.extractAndSavePatchs(userName,accessToken,oids);

                }).then();
    }
}
