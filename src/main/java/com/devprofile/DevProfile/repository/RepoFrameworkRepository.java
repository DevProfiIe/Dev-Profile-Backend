package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.RepoFrameworkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepoFrameworkRepository extends JpaRepository<RepoFrameworkEntity, Long> {
    List<RepoFrameworkEntity> findAllByRepoName(String repoName);
}

