package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface GitRepository extends JpaRepository<RepositoryEntity, String> {
    @Query("SELECT r.repoNodeId FROM RepositoryEntity r WHERE r.repoNodeId IN :repoNodeIds")
    List<String> findExistingRepoNodeIds(@Param("repoNodeIds") List<String> repoNodeIds);

    @Modifying
    @Query("update RepositoryEntity ur set ur.startDate = (select min(uc.commitDate) from CommitEntity uc where uc.repoName = ur.repoName), ur.endDate = (select max(uc.commitDate) from CommitEntity uc where uc.repoName = ur.repoName)")
    void updateStartDateEndDate();
    Optional<RepositoryEntity> findByRepoName(String repoName);

    @Query("SELECT r FROM RepositoryEntity r WHERE r.totalCommitCnt IS NOT NULL AND r.endDate IS NOT NULL")
    List<RepositoryEntity> findWithCommitAndEndDate();

}
