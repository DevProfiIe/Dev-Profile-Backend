package com.devprofile.DevProfile.service;

import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommitSaveService {

    private final ResourceLoader resourceLoader;
    private final WebClient.Builder webClientBuilder;

    private final CommitRepository commitRepository;


    @PostConstruct
    public void init() {
        webClient = webClientBuilder.baseUrl("https://api.github.com").build();
    }

    public class CustomGraphQLRequest{
        private final String query;
        private final Map<String, Object> variables;

        public CustomGraphQLRequest(String query, Map<String, Object> variables) {
            this.query = query;
            this.variables = variables;
        }

        public String getQuery() {
            return query;
        }

        public Map<String, Object> getVariables() {
            return variables;
        }
    }

    private WebClient webClient ;
    private String getGraphQLQuery(String fileName) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:graphqls/" + fileName);
        InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
        Scanner s = new Scanner(reader).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @Transactional
    public void saveCommits(List<CommitEntity> commits) {
        commitRepository.saveAll(commits);
        commitRepository.flush();
    }


    public Mono<Void> saveCommitsForRepo(UserEntity user) throws IOException {
        Integer userId = user.getId();
        String userNodeId = user.getNode_id();
        String userName = user.getName();

        String queryTemplate = getGraphQLQuery("commits_query_graphqls");


        Map<String, Object> variables = new HashMap<>();
        variables.put("login", userName);
        variables.put("node_id", userNodeId);


        CustomGraphQLRequest request = new CustomGraphQLRequest(queryTemplate, variables);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonRequest = objectMapper.writeValueAsString(request);

        Mono<JsonNode> responseMono = webClient.post()
                .uri("/graphql")
                .header("Authorization", "Bearer " + user.getGitHubToken())
                .bodyValue(jsonRequest)
                .retrieve()
                .bodyToMono(JsonNode.class);
        return responseMono.doOnNext(response -> {
            if (response.has("errors")) {
                System.out.println("GraphQL Errors: " + response.get("errors"));
            }
            extractAndSaveCommits(response, userId);
        }).then();
    }
    public void extractAndSaveCommits(JsonNode jsonResponse, Integer userId) {
        List<CommitEntity> commitsToSave = new ArrayList<>();
        List<String> allShas = new ArrayList<>();
        log.info("graph ql start");
        Iterator<JsonNode> commitsIterator = jsonResponse
                .path("data")
                .path("user")
                .path("repositories")
                .path("nodes")
                .elements();

        while (commitsIterator.hasNext()) {
            JsonNode commitNode = commitsIterator.next()
                    .path("defaultBranchRef")
                    .path("target")
                    .path("history")
                    .path("edges");

            for (JsonNode node : commitNode) {
                JsonNode commitInfo = node.path("node");
                String sha = commitInfo.get("oid").asText();
                allShas.add(sha);
            }
        }

        List<String> existingShas = commitRepository.findExistingShas(allShas);

        commitsIterator = jsonResponse
                .path("data")
                .path("user")
                .path("repositories")
                .path("nodes")
                .elements();

        while (commitsIterator.hasNext()) {
            JsonNode commitNode = commitsIterator.next()
                    .path("defaultBranchRef")
                    .path("target")
                    .path("history")
                    .path("edges");

            for (JsonNode node : commitNode) {
                JsonNode commitInfo = node.path("node");
                String sha = commitInfo.get("oid").asText();
                String message = commitInfo.get("message").asText();
                String date = commitInfo.path("author").get("date").asText();

                if (!existingShas.contains(sha)) {
                    CommitEntity commit = new CommitEntity();
                    commit.setCommitMessage(message);
                    commit.setCommitDate(date);
                    commit.setUserId(userId);
                    commit.setCommitSha(sha);
                    commitsToSave.add(commit);
                }
            }
        }

        if (!commitsToSave.isEmpty()) {
            saveCommits(commitsToSave);
        }
    }

}