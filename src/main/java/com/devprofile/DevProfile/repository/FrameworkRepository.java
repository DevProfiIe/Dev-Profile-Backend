package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.FrameworkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FrameworkRepository extends JpaRepository<FrameworkEntity, Long> {

    List<FrameworkEntity> findAllByFrameworkNameIn(List<String> frameworkNames);
}
