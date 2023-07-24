package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.CommitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface CommitRepository extends JpaRepository<CommitEntity, String> {
    



    @Query("select c from CommitEntity c where lower(c.commitMessage) like lower(:query)")
    List<CommitEntity> findExistingCommitMessage(@Param("query") String query);

    @Query("SELECT c.commitOid FROM CommitEntity c WHERE c.commitOid IN :oids")
    List<String> findExistingOids(@Param("oids") List<String> oids);

    @Query("SELECT c FROM CommitEntity c WHERE c.commitOid = :commitOid")
    Optional<CommitEntity> findByCommitOid(@Param("commitOid") String commitOid);
}