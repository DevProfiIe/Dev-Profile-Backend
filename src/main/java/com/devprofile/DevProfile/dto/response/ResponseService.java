package com.devprofile.DevProfile.dto.response;

import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.entity.CommitKeywordsEntity;
import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.CommitKeywordsRepository;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.devprofile.DevProfile.repository.GitRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ResponseService {
    private final CommitRepository commitRepository;
    private final CommitKeywordsRepository commitKeywordsRepository;

    public ResponseService(CommitRepository commitRepository, CommitKeywordsRepository commitKeywordsRepository) {
        this.commitRepository = commitRepository;
        this.commitKeywordsRepository = commitKeywordsRepository;

    }


    public Optional<CommitKeywordsDTO> getFeatureFramework(String commitOid) {
        Optional<CommitEntity> commitEntity = commitRepository.findByCommitOid(commitOid);
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


