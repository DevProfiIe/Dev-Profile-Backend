package com.devprofile.DevProfile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import nl.martijndwars.webpush.Subscription;

@Entity
@Getter
@Setter
@Table(name = "user_subscription")
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String endPoint;
    @Column
    private String userName;

    @Embedded
    private Keys keys;

    @Embeddable
    @Getter
    @Setter
    public static class Keys {
        private String p256dh;
        private String auth;

    }

    public Subscription toSubscription() {
        return new Subscription(endPoint, new Subscription.Keys(keys.getP256dh(), keys.getAuth()));
    }

}
