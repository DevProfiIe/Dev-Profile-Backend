package com.devprofile.DevProfile.service;


import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.repository.CommitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
    public void saveCommitDatas(Integer userId, List<String> commitMessages, List<String> commitDates, String userName,List<String> commitShas) {
        List<CommitEntity> commitEntities = new ArrayList<>();
        for (int i = 0; i < commitShas.size(); i++) {
            if (!commitRepository.existsByCommitSha(commitShas.get(i))) {
                    CommitEntity commitEntity = new CommitEntity(userId, commitMessages.get(i), commitDates.get(i),userName,commitShas.get(i));
                    commitEntities.add(commitEntity);
            }
        }
            commitRepository.saveAll(commitEntities);

    }
}
