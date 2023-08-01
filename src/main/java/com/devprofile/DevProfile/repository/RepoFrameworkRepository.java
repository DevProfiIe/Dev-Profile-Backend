package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.RepoFrameworkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepoFrameworkRepository extends JpaRepository<RepoFrameworkEntity, Long> {
    List<RepoFrameworkEntity> findAllByRepoName(String repoName);

    boolean existsByRepoNameAndFramework(String repoName, String langFramework);
}

