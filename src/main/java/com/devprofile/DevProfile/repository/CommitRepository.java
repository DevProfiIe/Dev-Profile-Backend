package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.CommitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CommitRepository extends JpaRepository<CommitEntity, String> {


    @Query("select c from CommitEntity c where lower(c.commitMessage) like lower(:query)")
    List<CommitEntity> findExistingCommitMessage(@Param("query") String query);

    @Query("SELECT c.commitSha FROM CommitEntity c WHERE c.commitSha IN :shas")
    List<String> findExistingShas(@Param("shas") List<String> shas);
}