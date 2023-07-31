package com.devprofile.DevProfile.handler;

import com.devprofile.DevProfile.dto.ChatMessageDto;
import com.devprofile.DevProfile.dto.UserEntityDto;
import com.devprofile.DevProfile.entity.ChatMessage;
import com.devprofile.DevProfile.entity.ChatRoom;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.ChatMessageRepository;
import com.devprofile.DevProfile.repository.ChatRoomRepository;
import com.devprofile.DevProfile.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class ChatMessageHandler {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageHandler(ChatRoomRepository chatRoomRepository,
                              UserRepository userRepository,
                              ChatMessageRepository chatMessageRepository,
                              SimpMessagingTemplate messagingTemplate) {
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat")
    public void handleChat(@Payload String chatMessageDtoStr, SimpMessageHeaderAccessor headerAccessor) {
        try {
            // String 형태의 메시지를 DTO 객체로 변환
            ObjectMapper mapper = new ObjectMapper();
            ChatMessageDto chatMessageDto = mapper.readValue(chatMessageDtoStr, ChatMessageDto.class);

            // 채팅방과 사용자 정보를 조회
            ChatRoom chatRoom = chatRoomRepository.findById(chatMessageDto.getId())
                    .orElseThrow(() -> new RuntimeException("채팅방 정보를 찾을 수 없습니다."));
            Optional<UserEntity> senderOpt = userRepository.findById(chatMessageDto.getSender().getId());
            if (!senderOpt.isPresent()) {
                throw new RuntimeException("사용자 정보를 찾을 수 없습니다.");
            }
            UserEntity sender = senderOpt.get();

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setChatRoom(chatRoom);
            chatMessage.setSender(sender);
            chatMessage.setMessage(chatMessageDto.getMessage());
            chatMessage.setTimestamp(LocalDateTime.now());
            ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

            ChatMessageDto chatMessageDtoResponse = new ChatMessageDto();
            chatMessageDtoResponse.setId(savedMessage.getId());
            chatMessageDtoResponse.setMessage(savedMessage.getMessage());
            chatMessageDtoResponse.setTimestamp(savedMessage.getTimestamp());

            UserEntityDto userEntityDto = new UserEntityDto();
            userEntityDto.setId(savedMessage.getSender().getId());
            userEntityDto.setLogin(savedMessage.getSender().getLogin());

            chatMessageDtoResponse.setSender(userEntityDto);

            // 메시지를 브로커에 전달
            messagingTemplate.convertAndSend("/topic/messages", chatMessageDtoResponse);

        } catch (IOException e) {
            throw new RuntimeException("메시지 파싱에 실패하였습니다.", e);
        }
    }

}