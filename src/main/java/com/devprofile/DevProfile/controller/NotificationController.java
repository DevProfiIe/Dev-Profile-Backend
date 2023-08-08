package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.entity.AlarmNotification;
import com.devprofile.DevProfile.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/alarms")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/unread")
    public List<AlarmNotification> getUnreadNotifications(@RequestParam String userName) {
        return notificationRepository.findByUserNameAndIsReadFalse(userName);
    }

    @PutMapping("/read/{id}")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Long id) {
        Optional<AlarmNotification> optionalNotification = notificationRepository.findById(id);

        if (optionalNotification.isPresent()) {
            AlarmNotification alarmNotification = optionalNotification.get();
            alarmNotification.setRead(true);
            notificationRepository.save(alarmNotification);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Alarm notification not found", HttpStatus.NOT_FOUND);
        }
    }
}