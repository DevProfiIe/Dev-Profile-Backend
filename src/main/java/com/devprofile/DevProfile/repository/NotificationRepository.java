package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.AlarmNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<AlarmNotification, Long> {
    List<AlarmNotification> findByUserNameAndIsReadFalse(String userName);
}
