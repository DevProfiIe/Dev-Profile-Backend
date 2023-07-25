package com.devprofile.DevProfile.service.commit;

import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.devprofile.DevProfile.repository.GitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class CommitUserService {

    private final CommitRepository commitRepository;
    private final GitRepository gitRepository;

    @Transactional
    public Map<String, List<String>> saveCommits(JsonNode repositories, String userName, Integer userId) {
        Map<String, List<String>> repoOidsMap = new HashMap<>();
        try {
            List<CommitEntity> commits = new ArrayList<>();
            for (JsonNode repo : repositories) {
                List<String> oids = new ArrayList<>();
                JsonNode edges = repo.get("defaultBranchRef").get("target").get("history").get("edges");
                for (JsonNode edge : edges) {
                    String oid = edge.get("node").get("oid").asText();

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
                }
                repoOidsMap.put(repo.get("name").asText(), oids);
            }
            commitRepository.saveAll(commits);
            commitRepository.flush();
        } catch (DataAccessException e) {
            log.error("Error saving commits: ", e);
        }
        return repoOidsMap;
    }

    @Transactional
    public void updateDates() {
        gitRepository.updateStartDateEndDate();
    }
}
