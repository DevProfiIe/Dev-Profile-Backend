package com.devprofile.DevProfile.service.commit;


import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommitOrgService {

    private final CommitRepository commitRepository;

    @Transactional
    public void saveCommits(JsonNode repositories, String userName, Integer userId) {
        try {
            List<CommitEntity> commits = new ArrayList<>();

            for (JsonNode repo : repositories) {
                JsonNode repository = repo.get("repositories");
                JsonNode nodes = repository.get("nodes");
                for(JsonNode repoNode:nodes){
                    String repoName = repoNode.get("name").asText();
                    JsonNode defaultBranchRef = repoNode.get("defaultBranchRef");
                    if (defaultBranchRef != null) {
                        JsonNode target = defaultBranchRef.get("target");
                        if (target != null) {
                            JsonNode history = target.get("history");
                            if (history != null) {
                                JsonNode edges = history.get("edges");
                                if (edges != null) {
                                    for (JsonNode edge : edges) {
                                        CommitEntity commitEntity = new CommitEntity();
                                        JsonNode node = edge.get("node");
                                        if (node != null) {
                                            commitEntity.setCommitMessage(node.get("message").asText());
                                            JsonNode author = node.get("author");
                                            if (author != null) {
                                                commitEntity.setCommitDate(author.get("date").asText());
                                            }
                                            commitEntity.setCommitOid(node.get("oid").asText());
                                        }
                                        commitEntity.setRepoNodeId(repoNode.get("id").asText());
                                        commitEntity.setRepoName(repoName);
                                        commitEntity.setUserName(userName);
                                        commitEntity.setUserId(userId);
                                        commits.add(commitEntity);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            commitRepository.saveAll(commits);
        } catch (DataAccessException e) {
            log.error("Error saving commits: ", e);
        }
    }
}