package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<PushSubscription,Long> {
    Optional<PushSubscription> findByUserName(String userName);
}
