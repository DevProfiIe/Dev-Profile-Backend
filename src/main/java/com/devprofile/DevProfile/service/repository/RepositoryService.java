package com.devprofile.DevProfile.service.repository;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryService {
    private final GitRepository gitRepository;

    @Transactional
    public void saveRepositories(List<RepositoryEntity> repositories) {
        gitRepository.saveAll(repositories);
        gitRepository.flush();
    }

        public void extractAndSaveRepositories(JsonNode jsonResponse, Integer userId) {
        List<RepositoryEntity> repositoriesToSave = new ArrayList<>();
        List<String> allRepoNodeIds = new ArrayList<>();
        Iterator<JsonNode> repositoriesIterator = jsonResponse
                .path("data")
                .path("user")
                .path("repositories")
                .path("nodes")
                .elements();

        while (repositoriesIterator.hasNext()) {
            JsonNode repoNode = repositoriesIterator.next();
            String repoNodeId = repoNode.get("id").asText();
            String repoName = repoNode.get("name").asText();
            allRepoNodeIds.add(repoNodeId);
        }

        List<String> existingRepoNodeIds = gitRepository.findExistingRepoNodeIds(allRepoNodeIds);

        repositoriesIterator = jsonResponse
                .path("data")
                .path("user")
                .path("repositories")
                .path("nodes")
                .elements();


        while (repositoriesIterator.hasNext()) {
            JsonNode repoNode = repositoriesIterator.next();
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
                repositoriesToSave.add(repository);
            }
        }

        if (!repositoriesToSave.isEmpty()) {
            saveRepositories(repositoriesToSave);
        }

    }

}