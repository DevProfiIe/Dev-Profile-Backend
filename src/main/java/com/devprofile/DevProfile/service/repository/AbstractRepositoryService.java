package com.devprofile.DevProfile.service.repository;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public abstract class AbstractRepositoryService {
    protected final GitRepository gitRepository;
    protected final WebClient webClient;

    public AbstractRepositoryService(GitRepository gitRepository, WebClient.Builder webClientBuilder) {
        this.gitRepository = gitRepository;
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
    }
    @Transactional
    public Mono<Void> saveRepositoryes(List<RepositoryEntity> repositories, String userId) {
        return Mono.fromRunnable(() -> {
            gitRepository.saveAll(repositories);
            gitRepository.flush();
        });
    }


    protected Mono<ClientResponse> executeGraphQLQuery(String query, Map<String, Object> variables) {
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("variables", variables);

        return webClient.post()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(body)
                .exchange();
    }

}
