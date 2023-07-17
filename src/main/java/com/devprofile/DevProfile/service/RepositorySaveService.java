package com.devprofile.DevProfile.service;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RepositorySaveService {
    @Autowired
    private final GitRepository gitRepository;

    public RepositorySaveService(GitRepository gitRepository) {
        this.gitRepository = gitRepository;
    }

    @Transactional
    public void saveRepoNames(List<String> repoNames, Integer userId, String userName) {
        List<RepositoryEntity> repositoryEntities = new ArrayList<>();
        for (String repoName : repoNames) {
            RepositoryEntity repositoryEntity = new RepositoryEntity(repoName, userId, userName);
            repositoryEntities.add(repositoryEntity);
        }
        gitRepository.saveAll(repositoryEntities);
    }


}
