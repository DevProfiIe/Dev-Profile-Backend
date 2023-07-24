package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.PatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatchRepository extends JpaRepository<PatchEntity,String> {
    @Query("SELECT c.commitOid FROM CommitEntity c WHERE c.commitOid IN :oids")
    List<String> findExistingOids(@Param("oids") List<String> oids);
}
