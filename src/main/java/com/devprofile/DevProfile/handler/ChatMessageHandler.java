package com.devprofile.DevProfile.handler;

import com.devprofile.DevProfile.dto.response.chat.ChatMessageDto;
import com.devprofile.DevProfile.dto.response.chat.UserEntityDto;
import com.devprofile.DevProfile.entity.ChatMessage;
import com.devprofile.DevProfile.entity.ChatRoom;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.ChatMessageRepository;
import com.devprofile.DevProfile.repository.ChatRoomRepository;
import com.devprofile.DevProfile.repository.UserRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

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
    public void handleChat(@Payload ChatMessageDto chatMessageDto, SimpMessageHeaderAccessor headerAccessor) {
        System.out.println("Test");
        ChatRoom chatRoom = chatRoomRepository.findById(chatMessageDto.getChatRoomId())
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
        System.out.println("chatMessage = " + chatMessage);
        chatMessage.setTimestamp(LocalDateTime.now());
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        ChatMessageDto chatMessageDtoResponse = new ChatMessageDto();
        chatMessageDtoResponse.setId(savedMessage.getId());
        chatMessageDtoResponse.setMessage(savedMessage.getMessage());
        chatMessageDtoResponse.setTimestamp(savedMessage.getTimestamp());
        chatMessageDtoResponse.setChatRoomId(savedMessage.getChatRoom().getId());

        UserEntityDto userEntityDto = new UserEntityDto();
        userEntityDto.setId(savedMessage.getSender().getId());
        userEntityDto.setLogin(savedMessage.getSender().getLogin());

        chatMessageDtoResponse.setSender(userEntityDto);

        // 메시지를 브로커에 전달
        messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoom.getId(), chatMessageDtoResponse);
    }
}