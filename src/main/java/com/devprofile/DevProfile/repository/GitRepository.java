package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GitRepository extends JpaRepository<RepositoryEntity, Integer> {
    Optional<RepositoryEntity> findById(Integer userId);
}
