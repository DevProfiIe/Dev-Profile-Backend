package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.ChatMessage;

public interface MessagePublisher {
    void publish(ChatMessage chatMessage);
}
