package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.ListEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListRepository extends MongoRepository<ListEntity,String> {
    List<ListEntity> findBySendUserLogin(String sendUserLogin);
    List<ListEntity> findByReceiveUserLogin(String receiveUserLogin);

}
