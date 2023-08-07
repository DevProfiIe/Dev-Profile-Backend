package com.devprofile.DevProfile.service.notification;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Security;

@Service
public class PushNotificationService {

    @Value("${vapid.public-key}")
    private String publicKey;

    @Value("${vapid.private-key}")
    private String privateKey;

    public PushNotificationService() {
        Security.addProvider(new BouncyCastleProvider());
    }

    public void sendPushMessage(Subscription subscription, byte[] payload) throws Exception {
        PushService pushService = new PushService(publicKey, privateKey);
        String payloadStr = new String(payload, StandardCharsets.UTF_8);
        Notification notification = new Notification(subscription, payloadStr);
        pushService.send(notification);
    }
}

