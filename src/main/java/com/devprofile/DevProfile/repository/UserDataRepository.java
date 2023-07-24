package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.UserDataEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserDataRepository extends MongoRepository<UserDataEntity, String> {
    UserDataEntity findByUserName(String userName);
}
