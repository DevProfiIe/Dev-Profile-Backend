package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.FilterEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilterRepository extends MongoRepository<FilterEntity, String> {
    FilterEntity findByUserLogin(String userLogin);


    @Query()
    Page<FilterEntity> findByFrameworksAndLanguages(List<String> framework, List<String> language,String field,Pageable pageable);
}
