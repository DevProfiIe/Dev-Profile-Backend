package com.devprofile.DevProfile.service.commit;

import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.repository.CommitRepository;
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
public class CommitOrgService {

    private final CommitRepository commitRepository;
    private final CommitService commitService;

    @Transactional
    public void saveCommits(List<CommitEntity> commits) {
        commitRepository.saveAll(commits);
        commitRepository.flush();
    }

    @Transactional
    public Map<String, Map<String, List<String>>> extractAndSaveOrgCommits(JsonNode jsonResponse, Integer userId) {
        List<CommitEntity> commitsToSave = new ArrayList<>();
        Map<String, Map<String, List<String>>> orgOidsMap = new HashMap<>();

        Iterator<JsonNode> orgsIterator = jsonResponse
                .path("data")
                .path("user")
                .path("organizations")
                .path("nodes")
                .elements();

        while (orgsIterator.hasNext()) {
            JsonNode orgNode = orgsIterator.next();
            String orgName = orgNode.get("name").asText();
            Map<String, List<String>> repoOidsMap = new HashMap<>();
            commitService.processCommits(orgNode.path("repositories").path("nodes")
                    .elements(), commitsToSave, repoOidsMap, userId);
            orgOidsMap.put(orgName, repoOidsMap);
        }

        try {
            if (!commitsToSave.isEmpty()) {
                saveCommits(commitsToSave);
            }
        } catch (DataAccessException ex) {
            log.error("Error occurred while saving commits", ex);
        }

        return orgOidsMap;
    }
}

