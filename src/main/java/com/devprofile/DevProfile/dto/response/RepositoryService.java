package com.devprofile.DevProfile.dto.response;

import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.entity.RepositoryEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RepositoryService {
    public List<RepositoryEntityDTO> createRepositoryEntityDTOs(List<RepositoryEntity> repositoryEntities, List<CommitEntity> commitEntities, Map<String, CommitKeywordsDTO> oidAndKeywordsMap) {
        List<RepositoryEntityDTO> extendedEntities = new ArrayList<>();

        if(commitEntities == null || repositoryEntities == null || oidAndKeywordsMap == null) {
            return extendedEntities;
        }

        Map<String, CommitEntity> oidAndCommitMap = commitEntities.stream()
                .collect(Collectors.toMap(CommitEntity::getCommitOid, Function.identity()));

        for (RepositoryEntity repositoryEntity : repositoryEntities) {
            RepositoryEntityDTO dto = new RepositoryEntityDTO();

            dto.setId(repositoryEntity.getId());
            dto.setRepoName(repositoryEntity.getRepoName());
            dto.setStartDate(repositoryEntity.getStartDate());
            dto.setEndDate(repositoryEntity.getEndDate());
            dto.setTotalCommitCnt(repositoryEntity.getTotalCommitCnt());
            dto.setMyCommitCnt(repositoryEntity.getMyCommitCnt());
            dto.setTotalContributors(repositoryEntity.getTotalContributors());
            dto.setRepoLanguages(repositoryEntity.getRepoLanguages());
            dto.setRepoDesc(repositoryEntity.getRepoDesc());

            for (Map.Entry<String, CommitKeywordsDTO> entry : oidAndKeywordsMap.entrySet()) {
                CommitEntity commitEntity = oidAndCommitMap.get(entry.getKey());

                if (commitEntity != null && commitEntity.getRepoName().equals(repositoryEntity.getRepoName())) {
                    dto.setFeatured(entry.getValue().getFeatured() != null ? new ArrayList<>(entry.getValue().getFeatured()) : new ArrayList<>());
                    dto.setLangFramework(entry.getValue().getLangFramework() != null ? new ArrayList<>(entry.getValue().getLangFramework()) : new ArrayList<>());
                    break;
                }
            }
            extendedEntities.add(dto);
        }
        return extendedEntities;
    }
}