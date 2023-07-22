package com.devprofile.DevProfile.service.commit;

import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommitService {

    private final CommitRepository commitRepository;

    @Transactional
    public void saveCommits(List<CommitEntity> commits) {
        commitRepository.saveAll(commits);
        commitRepository.flush();
    }

    public Map<String, List<String>> extractAndSaveCommits(JsonNode jsonResponse, Integer userId) {
        List<CommitEntity> commitsToSave = new ArrayList<>();
        List<String> allShas = new ArrayList<>();
        Map<String, List<String>> repoOidsMap = new HashMap<>();
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
            JsonNode repoNode = commitsIterator.next();
            String repoNodeId = repoNode.get("id").asText();
            String repoName = repoNode.get("name").asText();

            JsonNode commitNode = repoNode
                    .path("defaultBranchRef")
                    .path("target")
                    .path("history")
                    .path("edges");

            List<String> oids = new ArrayList<>();
            for (JsonNode node : commitNode) {
                JsonNode commitInfo = node.path("node");
                String sha = commitInfo.get("oid").asText();
                String message = commitInfo.get("message").asText();
                String date = commitInfo.path("author").get("date").asText();
                oids.add(sha);

                if (!existingShas.contains(sha)) {
                    CommitEntity commit = new CommitEntity();
                    commit.setCommitMessage(message);
                    commit.setCommitDate(date);
                    commit.setUserId(userId);
                    commit.setCommitSha(sha);
                    commit.setRepoNodeId(repoNodeId);
                    commit.setRepoName(repoName);
                    commitsToSave.add(commit);
                }
            }
            repoOidsMap.put(repoName, oids);
        }

        if (!commitsToSave.isEmpty()) {
            saveCommits(commitsToSave);
        }
        return repoOidsMap;
    }

}
