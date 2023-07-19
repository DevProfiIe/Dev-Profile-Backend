package com.devprofile.DevProfile.service;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RepositorySaveService {
    private final GitRepository gitRepository;

    @Transactional
    public void saveRepoNames(List<String> repoNames, Integer userId, String userName, List<Integer> repoId, List<String> repoNodeId) {
        for (int i = 0; i < repoNames.size(); i++) {
            if (!gitRepository.existsByRepoNodeId(repoNodeId.get(i))) {
                RepositoryEntity repositoryEntity = new RepositoryEntity(repoNames.get(i), userId, userName, repoId.get(i), repoNodeId.get(i));
                gitRepository.save(repositoryEntity);
            }
        }
    }
}
