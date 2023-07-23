package com.devprofile.DevProfile.service.repository;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@Slf4j
public class RepositoryService {
    private final GitRepository gitRepository;
    private final WebClient webClient;

    public RepositoryService(GitRepository gitRepository, WebClient.Builder webClientBuilder) {
        this.gitRepository = gitRepository;
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
    }
    @Transactional
    public void saveRepositories(List<RepositoryEntity> repositories) {
        gitRepository.saveAll(repositories);
        gitRepository.flush();
    }

    public List<RepositoryEntity> extractAndSaveRepositories(JsonNode jsonResponse, Integer userId) {
        List<String> allRepoNodeIds = getAllRepoNodeIds(jsonResponse);
        List<String> existingRepoNodeIds = gitRepository.findExistingRepoNodeIds(allRepoNodeIds);
        List<RepositoryEntity> repositoriesToSave = getRepositoriesToSave(jsonResponse, userId, existingRepoNodeIds);

        if (!repositoriesToSave.isEmpty()) {
            saveRepositories(repositoriesToSave);
        }

        return repositoriesToSave;
    }
    public List<String> getOrganizationNames(JsonNode jsonResponse) {
        List<String> organizationNames = new ArrayList<>();
        Iterator<JsonNode> iterator = jsonResponse.path("data").path("user").path("organizations").path("nodes").elements();

        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            String orgName = node.get("name").asText();
            organizationNames.add(orgName);
        }

        return organizationNames;
    }

    private List<String> getAllRepoNodeIds(JsonNode jsonResponse) {
        List<String> allRepoNodeIds = new ArrayList<>();
        allRepoNodeIds.addAll(getRepoNodeIds(jsonResponse, "repositories"));
        allRepoNodeIds.addAll(getRepoNodeIds(jsonResponse, "organizations"));
        return allRepoNodeIds;
    }

    private List<String> getRepoNodeIds(JsonNode jsonResponse, String path) {
        List<String> repoNodeIds = new ArrayList<>();
        Iterator<JsonNode> iterator = jsonResponse.path("data").path("user").path(path).path("nodes").elements();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();

            if ("repositories".equals(path)) {
                String repoNodeId = node.get("id").asText();
                repoNodeIds.add(repoNodeId);
            } else if ("organizations".equals(path)) {
                Iterator<JsonNode> orgIterator = node.path("repositories").path("nodes").elements();
                while (orgIterator.hasNext()) {
                    JsonNode repoNode = orgIterator.next();
                    String repoNodeId = repoNode.get("id").asText();
                    repoNodeIds.add(repoNodeId);
                }
            }
        }

        return repoNodeIds;
    }

    private List<RepositoryEntity> getRepositoriesToSave(JsonNode jsonResponse, Integer userId, List<String> existingRepoNodeIds) {
        List<RepositoryEntity> repositoriesToSave = new ArrayList<>();
        repositoriesToSave.addAll(getNewRepositories(jsonResponse, userId, existingRepoNodeIds, "organizations"));
        repositoriesToSave.addAll(getNewRepositories(jsonResponse, userId, existingRepoNodeIds, "repositories"));
        return repositoriesToSave;
    }

    private List<RepositoryEntity> getNewRepositories(JsonNode jsonResponse, Integer userId, List<String> existingRepoNodeIds, String path) {
        List<RepositoryEntity> repositories = new ArrayList<>();
        Iterator<JsonNode> iterator = jsonResponse.path("data").path("user").path(path).path("nodes").elements();

        while (iterator.hasNext()) {
            JsonNode node = iterator.next();

            if ("repositories".equals(path)) {
                createAndAddRepositoryIfNotExist(node, userId, existingRepoNodeIds, repositories);
            } else if ("organizations".equals(path)) {
                Iterator<JsonNode> orgIterator = node.path("repositories").path("nodes").elements();
                while (orgIterator.hasNext()) {
                    JsonNode orgNode = orgIterator.next();
                    orgCreateAndAddRepositoryIfNotExist(orgNode, userId, existingRepoNodeIds, repositories);
                }
            }
        }

        return repositories;
    }

    private void createAndAddRepositoryIfNotExist(JsonNode repoNode, Integer userId, List<String> existingRepoNodeIds, List<RepositoryEntity> repositories) {
        String repoNodeId = repoNode.get("id").asText();

        if (!existingRepoNodeIds.contains(repoNodeId)) {
            String repoName = repoNode.get("name").asText();
            String repoCreated = repoNode.get("createdAt").asText();
            String repoUpdated = repoNode.get("updatedAt").asText();
            String repoDesc = repoNode.get("description").asText(null);
            String repoUrl = repoNode.get("url").asText();

            RepositoryEntity repository = new RepositoryEntity();
            repository.setRepoName(repoName);
            repository.setRepoNodeId(repoNodeId);
            repository.setRepoCreated(repoCreated);
            repository.setRepoUpdated(repoUpdated);
            repository.setRepoDesc(repoDesc);
            repository.setRepoUrl(repoUrl);
            repository.setUserId(userId);
            repositories.add(repository);
        }
    }

    private void orgCreateAndAddRepositoryIfNotExist(JsonNode repoNode, Integer userId, List<String> existingRepoNodeIds, List<RepositoryEntity> repositories) {
        String repoNodeId = repoNode.get("id").asText();

        if (!existingRepoNodeIds.contains(repoNodeId)) {
            JsonNode commitHistory = repoNode.path("defaultBranchRef").path("target").path("history").path("edges");

            if (!commitHistory.isNull() && commitHistory.size() > 0) {
                String repoName = repoNode.get("name").asText();
                String repoCreated = repoNode.get("createdAt").asText();
                String repoUpdated = repoNode.get("updatedAt").asText();
                String repoDesc = repoNode.get("description").asText(null);
                String repoUrl = repoNode.get("url").asText();

                RepositoryEntity repository = new RepositoryEntity();
                repository.setRepoName(repoName);
                repository.setRepoNodeId(repoNodeId);
                repository.setRepoCreated(repoCreated);
                repository.setRepoUpdated(repoUpdated);
                repository.setRepoDesc(repoDesc);
                repository.setRepoUrl(repoUrl);
                repository.setUserId(userId);
                repositories.add(repository);
            }
        }
    }
}
