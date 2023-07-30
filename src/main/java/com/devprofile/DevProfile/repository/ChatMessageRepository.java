package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.ChatMessage;
import com.devprofile.DevProfile.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomOrderByTimestamp(ChatRoom chatRoom);
}
