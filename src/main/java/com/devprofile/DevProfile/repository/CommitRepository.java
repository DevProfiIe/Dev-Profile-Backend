package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.entity.RepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommitRepository extends JpaRepository<CommitEntity, Integer> {
    Optional<CommitEntity> findById(Integer userId);

    boolean existsByCommitSha(String commitSha);
}
