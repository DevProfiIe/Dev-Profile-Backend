package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.devprofile.DevProfile.service.RepositorySaveService;
import com.devprofile.DevProfile.service.CommitSaveService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {


    private final RestTemplate restTemplate;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final RepositorySaveService repositorySaveService;
    private final CommitSaveService commitSaveService;


    @GetMapping("/main")
    public ResponseEntity<ApiResponse<List<String>>> main(@AuthenticationPrincipal OAuth2User oauth2User, OAuth2AuthenticationToken authentication) {
        String userName = (String) oauth2User.getAttributes().get("login");
        Integer userId = (Integer) oauth2User.getAttributes().get("id");
        String apiUrl = "https://api.github.com/users/" + userName + "/repos";

        OAuth2AuthorizedClient authorizedClient =
                this.authorizedClientService.loadAuthorizedClient(
                        authentication.getAuthorizedClientRegistrationId(),
                        authentication.getName());
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(apiUrl, HttpMethod.GET, null, String.class);
            String response = responseEntity.getBody();

            ObjectMapper mapper = new ObjectMapper();
            List<String> repoNames = new ArrayList<>();
            JsonNode rootNode = mapper.readTree(response);
            if (rootNode.isArray()) {
                for (JsonNode repoNode : rootNode) {
                    String repoName = repoNode.path("name").asText();
                    repoNames.add(repoName);
                    commitSaveService.saveCommitsForRepo(repoName, oauth2User, accessToken);
                }
            }
            repositorySaveService.saveRepoNames(repoNames,userId,userName);

            ApiResponse<List<String>> apiResponse = new ApiResponse<>(true, repoNames, accessToken.getTokenValue(),  "Repository names retrieved successfully");
            return ResponseEntity.ok(apiResponse);
        } catch (RestClientException | IOException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<ApiResponse<List<String>>> handleException(Exception e) {
        String errorMessage = "Error during API call";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (e instanceof RestClientException) {
            errorMessage = "Error during calling GitHub API";
        } else if (e instanceof IOException) {
            errorMessage = "Error during parsing GitHub API response";
        }
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(false, null, null,  e.getMessage()));
    }



}