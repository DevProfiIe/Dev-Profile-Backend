package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.entity.ChatMessage;
import com.devprofile.DevProfile.entity.ChatRoom;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.ChatMessageRepository;
import com.devprofile.DevProfile.repository.ChatRoomRepository;
import com.devprofile.DevProfile.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatApiController {

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatApiController(UserRepository userRepository,
                             ChatRoomRepository chatRoomRepository,
                             ChatMessageRepository chatMessageRepository,
                             SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/chatroom/{userName}")
    public ResponseEntity<?> createChatRoom(@PathVariable String userName, Principal principal) {
        String currentUserName = principal.getName();
        UserEntity user1 = userRepository.findByLogin(currentUserName);
        UserEntity user2 = userRepository.findByLogin(userName);

        // 채팅방이 이미 존재하는지 확인
        ChatRoom existingChatRoom = chatRoomRepository.findByUser1AndUser2(user1, user2).orElse(null);
        if (existingChatRoom != null) {
            return ResponseEntity.ok(existingChatRoom);
        }

        // 채팅방 생성
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setUser1(user1);
        chatRoom.setUser2(user2);
        chatRoom.setCreatedAt(Instant.now());
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        return ResponseEntity.ok(savedChatRoom);
    }


    @PostMapping("/chatroom/{userName}/message")
    public ResponseEntity<?> sendMessage(@PathVariable String userName, @RequestBody ChatMessage message, Principal principal) {
        String currentUserName = principal.getName();
        UserEntity sender = userRepository.findByLogin(currentUserName);

        UserEntity receiver = userRepository.findByLogin(userName);

        ChatRoom chatRoom = chatRoomRepository.findById(message.getChatRoom().getId())
                .orElseThrow(() -> new RuntimeException("ChatRoom not found"));

        message.setSender(sender);
        message.setChatRoom(chatRoom);
        message.setTimestamp(Instant.now());
        ChatMessage savedMessage = chatMessageRepository.save(message);

        messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoom.getId(), savedMessage);

        return ResponseEntity.ok(savedMessage);
    }

    @GetMapping("/chatroom/{chatRoomId}/messages")
    public ResponseEntity<?> getChatRoomMessages(@PathVariable Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found"));

        List<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderByTimestamp(chatRoom);

        return ResponseEntity.ok(messages);
    }
}
