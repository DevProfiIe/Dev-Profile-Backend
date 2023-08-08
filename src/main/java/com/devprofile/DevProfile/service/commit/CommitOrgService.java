package com.devprofile.DevProfile.service.commit;


import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.devprofile.DevProfile.repository.GitRepository;
import com.devprofile.DevProfile.service.rabbitmq.MessageOrgSenderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommitOrgService {

    private final CommitRepository commitRepository;
    private final GitRepository gitRepository;
    private final MessageOrgSenderService messageOrgSenderService;
    private final ObjectMapper objectMapper;

    public Mono<Map<String, List<String>>> saveCommits(JsonNode repositories, String userName, Integer userId) {
        return Mono.defer(() -> {
            Map<String, List<String>> repoOidsMap = new HashMap<>();
            try {
                List<CommitEntity> commits = new ArrayList<>();

                for (JsonNode repo : repositories) {
                    List<String> oids = new ArrayList<>();
                    JsonNode repository = repo.get("repositories");
                    JsonNode nodes = repository.get("nodes");
                    for (JsonNode repoNode : nodes) {
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
                                            JsonNode node = edge.get("node");
                                            String commitOid = node.get("oid").asText();
                                            Optional<CommitEntity> existingCommit = commitRepository.findByCommitOid(commitOid);

                                            if (existingCommit.isPresent()) {
                                                continue;
                                            }

                                            CommitEntity commitEntity = new CommitEntity();

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
                                            commitEntity.setLength(0);
                                            commits.add(commitEntity);
                                            oids.add(commitOid);
                                            messageOrgSenderService.orgCommitSendMessage(commitEntity).subscribe(
                                                    result -> log.info("Sent message: " + result),
                                                    error -> log.error("Error sending message: ", error)
                                            );
                                        }
                                    }
                                }
                            }
                        }
                    }
                    repoOidsMap.put(repo.get("name").asText(), oids);
                }

                commitRepository.saveAll(commits);
                commitRepository.flush();
            } catch (DataAccessException e) {
                log.error("Error saving commits: ", e);
            }
            return Mono.just(repoOidsMap);
        });
    }

    @Transactional
    public Mono<Void> updateDates() {
        return Mono.defer(() -> {
            gitRepository.updateStartDateEndDate();

            try {
                ObjectNode messageObject = objectMapper.createObjectNode();
                messageObject.put("message", "Dates updated");
                String message = messageObject.toString();

                messageOrgSenderService.orgSendMessage(message).subscribe(
                        result -> log.info("Sent message: " + result),
                        error -> log.error("Error sending message: ", error)
                );
            } catch (Exception e) {
                log.info(e.getMessage());
                e.printStackTrace();
            }
            return Mono.empty();
        });
    }
}