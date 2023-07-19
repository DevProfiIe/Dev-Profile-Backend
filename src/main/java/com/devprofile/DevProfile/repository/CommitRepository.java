package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.CommitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface CommitRepository extends JpaRepository<CommitEntity, String> {

    boolean existsByCommitSha(String commitSha);
}
