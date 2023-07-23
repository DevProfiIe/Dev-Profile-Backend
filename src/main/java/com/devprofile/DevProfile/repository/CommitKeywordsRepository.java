package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.CommitKeywordsEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommitKeywordsRepository extends MongoRepository<CommitKeywordsEntity, String> {

}
