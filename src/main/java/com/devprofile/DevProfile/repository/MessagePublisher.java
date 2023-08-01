package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.ChatMessage;
import org.springframework.stereotype.Repository;

@Repository
public interface MessagePublisher {
    void publish(ChatMessage chatMessage);
}
