package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.ListEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ListRepository extends MongoRepository<ListEntity,String> {
    public List<ListEntity> findBySendUserLogin(String userLogin);

    public List<ListEntity> findByReceiveUserLogin(String userLogin);

}
