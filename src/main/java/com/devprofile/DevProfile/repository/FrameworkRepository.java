package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.FrameworkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface FrameworkRepository extends JpaRepository<FrameworkEntity, Integer> {

    List<FrameworkEntity> findAll();
}
