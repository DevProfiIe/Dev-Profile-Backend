package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.UserDataEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDataRepository extends MongoRepository<UserDataEntity, String> {
    UserDataEntity findByUserName(String userName);
}
