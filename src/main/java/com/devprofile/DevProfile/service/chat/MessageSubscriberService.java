package com.devprofile.DevProfile.service.chat;

import com.devprofile.DevProfile.entity.ChatMessage;
import com.devprofile.DevProfile.repository.ChatMessageRepository;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

@Service
public class MessageSubscriberService implements MessageListener {
    private final ChatMessageRepository chatMessageRepository;

    public MessageSubscriberService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        ChatMessage chatMessage = (ChatMessage) SerializationUtils.deserialize(message.getBody());
        chatMessageRepository.save(chatMessage);
    }
}
