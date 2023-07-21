package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface GitRepository extends JpaRepository<RepositoryEntity, String> {
    @Query("SELECT r.repoNodeId FROM RepositoryEntity r WHERE r.repoNodeId IN :repoNodeIds")
    List<String> findExistingRepoNodeIds(@Param("repoNodeIds") List<String> repoNodeIds);
}
