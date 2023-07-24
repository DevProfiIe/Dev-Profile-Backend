package com.devprofile.DevProfile.dto.response;

import com.devprofile.DevProfile.entity.RepositoryEntity;

import java.util.List;
import java.util.Map;

public class CombinedResponseDTO {
    private Map<String, CommitKeywordsDTO> oidAndKeywordsMap;
    private List<RepositoryEntity> repositoryEntities;

    public Map<String, CommitKeywordsDTO> getOidAndKeywordsMap() {
        return oidAndKeywordsMap;
    }

    public void setOidAndKeywordsMap(Map<String, CommitKeywordsDTO> oidAndKeywordsMap) {
        this.oidAndKeywordsMap = oidAndKeywordsMap;
    }

    public List<RepositoryEntity> getRepositoryEntities() {
        return repositoryEntities;
    }

    public void setRepositoryEntities(List<RepositoryEntity> repositoryEntities) {
        this.repositoryEntities = repositoryEntities;
    }
}
