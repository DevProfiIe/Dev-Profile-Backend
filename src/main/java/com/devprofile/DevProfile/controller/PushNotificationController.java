package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.entity.PushSubscription;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.SubscriptionRepository;
import com.devprofile.DevProfile.repository.UserRepository;
import com.devprofile.DevProfile.service.notification.PushNotificationService;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Subscription;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/push")
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;



    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestParam String userName, @RequestBody PushSubscription pushSubscription) {
        UserEntity userEntity = userRepository.findByLogin(userName);
        if (userEntity == null) {
            return new ResponseEntity<>("User not found", HttpStatus.BAD_REQUEST);
        }

        pushSubscription.setUserName(userEntity.getLogin());

        Subscription subscription = new Subscription(
                pushSubscription.getEndPoint(),
                new Subscription.Keys(pushSubscription.getKeys().getP256dh(), pushSubscription.getKeys().getAuth())
        );

        subscriptionRepository.save(pushSubscription);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestParam String userName, @RequestBody String payload) {
        Optional<PushSubscription> pushSubscriptionOpt = subscriptionRepository.findByUserName(userName);
        if(!pushSubscriptionOpt.isPresent()) {
            return new ResponseEntity<>("Subscription not found", HttpStatus.NOT_FOUND);
        }

        PushSubscription pushSubscription = pushSubscriptionOpt.get();
        Subscription subscription = new Subscription(pushSubscription.getEndPoint(), new Subscription.Keys(pushSubscription.getKeys().getP256dh(), pushSubscription.getKeys().getAuth()));

        try {
            pushNotificationService.sendPushMessage(subscription, payload.getBytes());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
