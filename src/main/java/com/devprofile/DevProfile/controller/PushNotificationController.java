package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.UserRepository;
import com.devprofile.DevProfile.request.SubscriptionRequest;
import com.devprofile.DevProfile.service.notification.PushNotificationService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.TopicManagementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@RequiredArgsConstructor
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;

    private final UserRepository userRepository;

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribeAndSubscribeTopic(@RequestBody SubscriptionRequest subscriptionRequest) {
        String userName = subscriptionRequest.getUsername();
        String token = subscriptionRequest.getToken();

        UserEntity userEntity = userRepository.findByLogin(userName);
        if (userEntity != null) {
            userEntity.setToken(token);
            userRepository.save(userEntity);
            try {
                TopicManagementResponse response = FirebaseMessaging.getInstance().subscribeToTopic(Collections.singletonList(token), "your-topic-name");
                System.out.println("구독 성공: " + response.getSuccessCount() + " devices");
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (FirebaseMessagingException e) {
                System.err.println("구독 실패: " + e.getErrorCode());
                return new ResponseEntity<>("Failed to subscribe to topic", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
    }

//    @PostMapping("/send-notification")
//    public ResponseEntity<?> sendNotification(@RequestBody PushNotificationRequest request) {
//        String recipientToken = userRepository.findTokenByUsername(request.getUserName());
//
//        if (recipientToken == null) {
//            return new ResponseEntity<>("Recipient token not found", HttpStatus.NOT_FOUND);
//        }
//
//        try {
//            pushNotificationService.sendPushMessage(request);
//            return new ResponseEntity<>(HttpStatus.OK);
//        } catch (FirebaseMessagingException e) {
//            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
}
