package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.service.CommitSaveService2;
import com.devprofile.DevProfile.service.RepositorySaveService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.*;
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
import org.springframework.web.client.RestTemplate;
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
    public String main(@AuthenticationPrincipal OAuth2User oauth2User, OAuth2AuthenticationToken authentication) {
        String userName = (String) oauth2User.getAttributes().get("login");
        Integer userId = (Integer) oauth2User.getAttributes().get("id");
        String apiUrl = "https://api.github.com/search/commits?q=author:" + userName;
        OAuth2AuthorizedClient authorizedClient =
                this.authorizedClientService.loadAuthorizedClient(
                        authentication.getAuthorizedClientRegistrationId(),
                        authentication.getName());
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        System.out.println("accessToken = " + accessToken.getTokenValue());


        fetchAllPages(apiUrl, accessToken, userName, userId);

        return "main";
    }

    private void fetchAllPages(String apiUrl, OAuth2AccessToken accessToken, String userName, Integer userId) {
        try{
            GitHub gitHub = new GitHubBuilder().withOAuthToken(accessToken.getTokenValue()).build();
            gitHub.checkApiUrlValidity();

            GHCommitSearchBuilder builder = gitHub.searchCommits()
                    .author(userName)
                    .sort(GHCommitSearchBuilder.Sort.AUTHOR_DATE);
            PagedSearchIterable<GHCommit> commits = builder.list().withPageSize(30);

            for (GHCommit commit : commits) {
                GHCommit.ShortInfo commitInfo = commit.getCommitShortInfo();


                CommitEntity commitEntity = new CommitEntity(
                        userId,
                        commitInfo.getMessage(),
                        commitInfo.getAuthor().getDate().toString(),
                        userName,
                        commit.getSHA1());

                commitSaveService2.saveCommitDatas(commitEntity);

                GHRepository repository = commit.getOwner();
                RepositoryEntity repositoryEntity = new RepositoryEntity(
                        repository.getName(),
                        userId,
                        userName,
                        (int) repository.getId(),
                        repository.getNodeId());
                repositorySaveService.saveRepoNames(repositoryEntity);
            }

        }catch (IOException e){
            log.error(e.getMessage());
        }




//        RestTemplate restTemplate = new RestTemplate();
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "Bearer " + accessToken.getTokenValue());
//        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
//
//        ResponseEntity<String> response = restTemplate.exchange(
//                apiUrl + "&per_page=1", HttpMethod.GET, entity, String.class);
//        int totalCount = 0;
//        ObjectMapper mapper = new ObjectMapper();
//        try {
//            JsonNode rootNode = mapper.readTree(response.getBody());
//            totalCount = rootNode.path("total_count").asInt();
//        } catch (IOException e) {
//            handleException(e);
//            return;
//        }
//        int pageNum = 1;
//        while (totalCount > 0) {
//            ResponseEntity<String> commitResponse = restTemplate.exchange(
//                    apiUrl + "&per_page=30&page=" + pageNum, HttpMethod.GET, entity, String.class);
//
//            try {
//                ObjectMapper mapping = new ObjectMapper();
//                JsonNode rootNode = mapping.readTree(commitResponse.getBody());
//                if(rootNode.path("incomplete_result").asBoolean()) log.error("약해");
//                if (rootNode.path("items").isArray()) {
//                    for (JsonNode commitNode : rootNode.path("items")) {
//                        String commitDate = commitNode.path("commit").path("author").path("date").asText();
//                        String commitMessage = commitNode.path("commit").path("message").asText();
//                        String commitSha = commitNode.path("sha").asText();
//                        CommitEntity commitEntity = new CommitEntity(userId, commitMessage, commitDate, userName, commitSha);
//                        commitSaveService2.saveCommitDatas(commitEntity);
//                        JsonNode repoNode = commitNode.path("repository");
//                        int repoId = repoNode.path("id").asInt();
//                        String repoNodeId = repoNode.path("node_id").asText();
//                        String repoName = repoNode.path("name").asText();
//                        RepositoryEntity repositoryEntity = new RepositoryEntity(repoName, userId, userName, repoId, repoNodeId);
//                        repositorySaveService.saveRepoNames(repositoryEntity);
//                    }
//                }
//            } catch (Exception e) {
//                log.error("Is there any Problem?!!");
//                return;
//            }
//            totalCount -= 30;
//            pageNum++;
//        }
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

