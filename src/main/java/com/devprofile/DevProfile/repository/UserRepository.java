package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    Optional<UserEntity> findById(Integer userId);
    boolean existsById(Integer userId);

    UserEntity findByLogin(String login);
    UserEntity findByJwtRefreshToken(String jwtRefreshToken);

    @Query("SELECT u.token FROM UserEntity u WHERE u.login = :username")
    String findTokenByUsername(@Param("username") String username);
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
