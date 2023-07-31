package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.WordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WordRepository extends JpaRepository<WordEntity, Integer> {
    List<WordEntity> findByFirstChar(char firstChar);
}
