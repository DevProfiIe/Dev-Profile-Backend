package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.CommitKeywordsEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommitKeywordsRepository extends MongoRepository<CommitKeywordsEntity, String> {
    CommitKeywordsEntity findByOid(String oid);
    List<CommitKeywordsEntity> findByFieldContaining(String field);
    List<CommitKeywordsEntity> findByUserName(String userName);
}
