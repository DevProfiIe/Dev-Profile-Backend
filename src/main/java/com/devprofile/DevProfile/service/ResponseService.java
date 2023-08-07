package com.devprofile.DevProfile.service;

import com.devprofile.DevProfile.dto.response.analyze.CommitKeywordsDTO;
import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.entity.CommitKeywordsEntity;
import com.devprofile.DevProfile.repository.CommitKeywordsRepository;
import com.devprofile.DevProfile.repository.CommitRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ResponseService {
    private final CommitRepository commitRepository;
    private final CommitKeywordsRepository commitKeywordsRepository;

    public ResponseService(CommitRepository commitRepository, CommitKeywordsRepository commitKeywordsRepository) {
        this.commitRepository = commitRepository;
        this.commitKeywordsRepository = commitKeywordsRepository;

    }


    public Optional<CommitKeywordsDTO> getFeatureFramework(String commitOid,Integer userId) {
        Optional<CommitEntity> commitEntity = commitRepository.findByCommitOidAndUserId(commitOid,userId);
        if(commitEntity.isPresent()){
            Optional<CommitKeywordsEntity> commitKeywordsEntity = Optional.ofNullable(commitKeywordsRepository.findByOid(commitOid));

            if(commitKeywordsEntity.isPresent()){
                CommitKeywordsDTO dto = new CommitKeywordsDTO();
                dto.setFeatured(commitKeywordsEntity.get().getFeatured());
                dto.setLangFramework(commitKeywordsEntity.get().getLangFramework());
                return Optional.of(dto);
            }
        }

        return Optional.empty();
    }
}


