package com.devprofile.DevProfile.service;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RepositorySaveService {
    private final GitRepository gitRepository;


    @Transactional
    public synchronized void saveRepoNames(RepositoryEntity repository) {
        if (!gitRepository.existsByRepoNodeId(repository.getRepoNodeId())) {
            gitRepository.save(repository);
        }
    }
}
