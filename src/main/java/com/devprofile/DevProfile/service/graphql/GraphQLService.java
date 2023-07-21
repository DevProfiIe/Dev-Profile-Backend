package com.devprofile.DevProfile.service.graphql;

import com.devprofile.DevProfile.entity.UserEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphQLService {

    private final WebClient.Builder webClientBuilder;
    private final ResourceLoader resourceLoader;
    private WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        webClient = webClientBuilder.baseUrl("https://api.github.com").build();
    }

    public static class CustomGraphQLRequest{
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

    public String getGraphQLQuery(String fileName) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:graphqls/" + fileName);
        InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
        Scanner s = new Scanner(reader).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public Mono<JsonNode> sendGraphQLRequest(UserEntity user, CustomGraphQLRequest request) throws JsonProcessingException {
        String jsonRequest = objectMapper.writeValueAsString(request);
        return webClient.post()
                .uri("/graphql")
                .header("Authorization", "Bearer " + user.getGitHubToken())
                .bodyValue(jsonRequest)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

}
