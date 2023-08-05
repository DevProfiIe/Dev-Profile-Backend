package com.devprofile.DevProfile.service.message;

import com.devprofile.DevProfile.entity.RepositoryEntity;

public class RepositoryMessage {
    private final RepositoryEntity repositoryEntity;

    public RepositoryMessage(RepositoryEntity repositoryEntity) {
        this.repositoryEntity = repositoryEntity;
    }

    public RepositoryEntity getRepositoryEntity() {
        return this.repositoryEntity;
    }
}
