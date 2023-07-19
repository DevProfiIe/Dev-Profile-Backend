package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface GitRepository extends JpaRepository<RepositoryEntity, String> {
    boolean existsByRepoNodeId(String repoNodeId);
}
