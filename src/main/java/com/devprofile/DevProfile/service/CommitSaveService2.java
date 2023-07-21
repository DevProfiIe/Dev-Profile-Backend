package com.devprofile.DevProfile.service;


import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.repository.CommitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommitSaveService2 {

    private final CommitRepository commitRepository;

    @Transactional
    public synchronized void saveCommitDatas(CommitEntity commit) {
        if (!commitRepository.existsByCommitSha(commit.getCommitSha())) {
            commitRepository.save(commit);
        }else{
            log.error(commit.getCommitMessage());
        }
    }
}
