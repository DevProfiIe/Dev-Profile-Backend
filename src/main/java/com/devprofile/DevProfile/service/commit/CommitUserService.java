package com.devprofile.DevProfile.service.commit;

import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.devprofile.DevProfile.repository.GitRepository;
import com.devprofile.DevProfile.service.rabbitmq.MessageSenderService;
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
public class CommitUserService {

    private final CommitRepository commitRepository;
    private final GitRepository gitRepository;
    private final MessageSenderService messageSenderService;
    private final ObjectMapper objectMapper;


    public Mono<Map<String, List<String>>> saveCommits(JsonNode repositories, String userName, Integer userId) {
        Map<String, List<String>> repoOidsMap = new HashMap<>();
        try {
            List<CommitEntity> commits = new ArrayList<>();
            for (JsonNode repo : repositories) {
                List<String> oids = new ArrayList<>();
                JsonNode defaultBranchRef = repo.get("defaultBranchRef");
                if (defaultBranchRef == null) {
                    continue;
                }
                JsonNode target = defaultBranchRef.get("target");
                if (target == null) {
                    continue;
                }
                JsonNode history = target.get("history");
                if (history == null) {
                    continue;
                }
                JsonNode edges = history.get("edges");
                if (edges == null) {
                    continue;
                }
                for (JsonNode edge : edges) {
                    JsonNode node = edge.get("node");
                    if (node == null) {
                        continue;
                    }
                    String oid = node.get("oid").asText();
                    if (oid == null) {
                        continue;
                    }
                    Optional<CommitEntity> existingCommit = commitRepository.findByCommitOid(oid);
                    if (existingCommit.isPresent()) {
                        continue;
                    }
                    CommitEntity commitEntity = new CommitEntity();
                    commitEntity.setCommitMessage(edge.get("node").get("message").asText());
                    commitEntity.setCommitDate(edge.get("node").get("author").get("date").asText());
                    commitEntity.setUserName(userName);
                    commitEntity.setCommitOid(oid);
                    commitEntity.setRepoNodeId(repo.get("id").asText());
                    String repoName = repo.get("name").asText();
                    commitEntity.setRepoName(repoName);
                    commitEntity.setUserId(userId);
                    commitEntity.setLength(0);
                    commits.add(commitEntity);
                    oids.add(oid);

                    messageSenderService.CommitSendMessage(commitEntity).subscribe(result -> log.info("Sent message: " + result));
                }
                repoOidsMap.put(repo.get("name").asText(), oids);
            }
            commitRepository.saveAll(commits);
            commitRepository.flush();
        } catch (DataAccessException e) {
            log.error("Error saving commits: ", e);
        }
        return Mono.just(repoOidsMap);
    }



    @Transactional
    public Mono<Void> updateDates() {
        gitRepository.updateStartDateEndDate();

        try {
            ObjectNode messageObject = objectMapper.createObjectNode();
            messageObject.put("message", "Dates updated");
            String message = messageObject.toString();

            messageSenderService.sendMessage(message).subscribe(result -> log.info("Sent message: " + result));
        }catch (Exception e) {
            log.info(e.getMessage());
            e.printStackTrace();
        }
        return Mono.empty();
    }


    public void saveCommits(CommitEntity commitEntity) {
    }
}