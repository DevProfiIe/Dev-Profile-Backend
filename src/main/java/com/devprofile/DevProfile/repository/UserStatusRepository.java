package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.UserDataEntity;
import com.devprofile.DevProfile.entity.UserStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface UserStatusRepository extends MongoRepository<UserStatusEntity, String> {
    public List<UserStatusEntity> findBySendUserLogin(String userName);
    public List<UserStatusEntity> findByReceiveUserLogin(String userName);
}
