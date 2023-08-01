package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.FilterEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilterRepository extends MongoRepository<FilterEntity, String> {
}
