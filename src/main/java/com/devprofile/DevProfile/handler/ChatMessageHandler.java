package com.devprofile.DevProfile.handler;

import com.devprofile.DevProfile.dto.ChatMessageDto;
import com.devprofile.DevProfile.entity.ChatMessage;
import com.devprofile.DevProfile.entity.ChatRoom;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.ChatMessageRepository;
import com.devprofile.DevProfile.repository.ChatRoomRepository;
import com.devprofile.DevProfile.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;


import java.io.IOException;
import java.time.Instant;

@Controller
public class ChatMessageHandler {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat")
    public void handleChat(@Payload String chatMessageDtoStr, SimpMessageHeaderAccessor headerAccessor) {
        try {
            // String 형태의 메시지를 DTO 객체로 변환
            ObjectMapper mapper = new ObjectMapper();
            ChatMessageDto chatMessageDto = mapper.readValue(chatMessageDtoStr, ChatMessageDto.class);

            // 채팅방과 사용자 정보를 조회
            ChatRoom chatRoom = chatRoomRepository.findById(Long.parseLong(chatMessageDto.getChatRoomId()))
                    .orElseThrow(() -> new RuntimeException("채팅방 정보를 찾을 수 없습니다."));
            UserEntity sender = userRepository.findByLogin(chatMessageDto.getSenderId());

            // 메시지를 생성하고 저장
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setChatRoom(chatRoom);
            chatMessage.setChatId(chatMessageDto.getFrom());
            chatMessage.setSender(sender);
            chatMessage.setMessage(chatMessageDto.getText());
            chatMessage.setTimestamp(Instant.now());
            chatMessageRepository.save(chatMessage);

            // 메시지를 브로커에 전달
            messagingTemplate.convertAndSend("/topic/messages", chatMessageDto);

        } catch (IOException e) {
            throw new RuntimeException("메시지 파싱에 실패하였습니다.", e);
        }
    }
}
