package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    Optional<UserEntity> findById(Integer id);
    boolean existsById(Integer id);

    UserEntity findByLogin(String login);
}
