package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.UserScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserScoreRepository extends JpaRepository<UserScore, Long> {
    List<UserScore> findByLogin(String login);
    List<UserScore> findAllByLogin(String login);
    UserScore findByFieldAndLogin(String field, String login);

}
