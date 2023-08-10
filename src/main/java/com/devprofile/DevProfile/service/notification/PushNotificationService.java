package com.devprofile.DevProfile.service.notification;

import com.devprofile.DevProfile.request.PushNotificationRequest;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.springframework.stereotype.Service;



@Service
public class PushNotificationService {

    public void sendPushMessage(PushNotificationRequest request) throws FirebaseMessagingException {
        Message message = Message.builder()
                .putData("title", request.getTitle())
                .putData("body", request.getBody())
                .build();

        FirebaseMessaging.getInstance().send(message);
    }
}

