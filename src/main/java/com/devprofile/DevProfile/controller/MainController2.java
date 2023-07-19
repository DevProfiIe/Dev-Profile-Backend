package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.service.CommitSaveService2;
import com.devprofile.DevProfile.service.RepositorySaveService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController2 {
    private static final Logger logger = LoggerFactory.getLogger(MainController2.class);
    private final WebClient webClient;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final RepositorySaveService repositorySaveService;
    private final CommitSaveService2 commitSaveService2;

    @GetMapping("/main")
    public List<String> main(@AuthenticationPrincipal OAuth2User oauth2User, OAuth2AuthenticationToken authentication) {
        String userName = (String) oauth2User.getAttributes().get("login");
        Integer userId = (Integer) oauth2User.getAttributes().get("id");
        String apiUrl = "https://api.github.com/search/commits?q=author:" + userName;
        OAuth2AuthorizedClient authorizedClient =
                this.authorizedClientService.loadAuthorizedClient(
                        authentication.getAuthorizedClientRegistrationId(),
                        authentication.getName());
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        System.out.println("accessToken = " + accessToken.getTokenValue());
        List<String> responseList =fetchAllPages(1,apiUrl, accessToken, new ArrayList<>());
                    List<String> commitDates = new ArrayList<>();
                    List<String> commitMessages = new ArrayList<>();
                    List<String> commitShas = new ArrayList<>();
                    List<Integer> repoIds = new ArrayList<>();
                    List<String> repoNodeIds = new ArrayList<>();
                    List<String> repoNames = new ArrayList<>();

                    for (String response : responseList) {
                        try {

                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode rootNode = mapper.readTree(response);
                            if (rootNode.path("items").isArray()) {
                                for (JsonNode commitNode : rootNode.path("items")) {
                                    String commitDate = commitNode.path("commit").path("author").path("date").asText();
                                    String commitMessage = commitNode.path("commit").path("message").asText();
                                    String commitSha = commitNode.path("sha").asText();
                                    commitDates.add(commitDate);
                                    commitMessages.add(commitMessage);
                                    commitShas.add(commitSha);
                                    JsonNode repoNode = commitNode.path("repository");
                                    int repoId = repoNode.path("id").asInt();
                                    String repoNodeId = repoNode.path("node_id").asText();
                                    String repoName = repoNode.path("name").asText();
                                    repoIds.add(repoId);
                                    repoNodeIds.add(repoNodeId);
                                    repoNames.add(repoName);
                                }
                            }
                        } catch (IOException e) {
                            return handleException(e);
                        }
                    }

                    repositorySaveService.saveRepoNames(repoNames, userId, userName, repoIds, repoNodeIds);
                    commitSaveService2.saveCommitDatas(userId,commitMessages,commitDates, userName,commitShas);

//                    ApiResponse<List<String>> apiResponse = new ApiResponse<>(true, repoNames, accessToken.getTokenValue(), "Repository and Commit data retrieved successfully");
//                    return Mono.just(ResponseEntity.ok(apiResponse));

                    return responseList;
    }

    private List<String> fetchAllPages(Integer pageNum,String apiUrl, OAuth2AccessToken accessToken, List<String> responses) {
        String response = webClient.get()
                .uri(apiUrl+"&page="+pageNum)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getTokenValue())
                .retrieve()
                .bodyToMono(String.class)
                .block();

                responses.add(response);

                ObjectMapper mapper = new ObjectMapper();
                try {
                    JsonNode rootNode = mapper.readTree(response);
                    int totalCount = rootNode.path("total_count").asInt();
                    int remainingCount = totalCount - (30 * pageNum);

                    if (remainingCount > 0) {
                        return fetchAllPages(pageNum+1,apiUrl, accessToken, responses);
                    } else {
                        return responses;
                    }
                } catch (IOException e) {
                    return handleException(e);
                }

    }

    private List<String> handleException(Throwable e) {
        logger.error("Error during API call", e);

        String errorMessage = "Error during API call";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (e instanceof RestClientException) {
            errorMessage = "GitHub API 호출 중 에러 발생";
        } else if (e instanceof IOException) {
            errorMessage = "GitHub API 응답 파싱 중 에러 발생";
        }

        List<String> errorResponse = new ArrayList<>();
        errorResponse.add(errorMessage);

        return errorResponse;
    }
}

