package com.devprofile.DevProfile.service;

import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class CommitSaveService {


    private final RestTemplate restTemplate;

    private final CommitRepository commitRepository;

    @Transactional
    public void saveCommits(String message, String date, Integer userId, String repoName, String commitSha) {
        if (!commitRepository.existsByCommitSha(commitSha)) {
            CommitEntity commit = new CommitEntity();
            commit.setCommitMessage(message);
            commit.setCommitDate(date);
            commit.setUserId(userId);
            commit.setRepositoryName(repoName);
            commit.setCommitSha(commitSha);
            commitRepository.save(commit);
        }
    }
    public void saveCommitsForRepo(String repoName, OAuth2User oauth2User, OAuth2AccessToken accessToken) {
        String userName = (String) oauth2User.getAttributes().get("login");
        Integer userId = (Integer) oauth2User.getAttributes().get("id");
        String apiUrl = "https://api.github.com/repos/" + userName + "/" + repoName + "/commits";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken.getTokenValue());

        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            int page = 1;
            while (true) {
                ResponseEntity<String> responseEntity = restTemplate.exchange(apiUrl + "?page=" + page, HttpMethod.GET, entity, String.class);
                String response = responseEntity.getBody();

                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response);

                if (!rootNode.isArray() || rootNode.size() == 0) {
                    break;
                }

                for (JsonNode commitNode : rootNode) {
                    JsonNode commitAuthor = commitNode.get("author");
                    JsonNode commitIdNode = commitAuthor.get("id");

                    if(commitIdNode!=null) {
                        String commitId = commitIdNode.asText();
                        if (commitId.equals(String.valueOf(userId))) {
                            JsonNode commit = commitNode.get("commit");
                            String commitMessage = commit.get("message").asText();
                            String commitDate = commit.get("committer").get("date").asText();
                            JsonNode commitSha = commitNode.get("sha");
                            saveCommits(commitMessage, commitDate, userId, repoName, String.valueOf(commitSha));
                        }
                    }
                }
                page++;
            }
        } catch (RestClientException | IOException e) {
            e.printStackTrace();
        }
    }
}
