package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    Optional<UserEntity> findById(Integer userId);
    boolean existsById(Integer userId);

    UserEntity findByLogin(String login);
    UserEntity findByJwtRefreshToken(String jwtRefreshToken);


    class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    class UserExistsException extends RuntimeException {
        public UserExistsException(String message) {
            super(message);
        }
    }
}
