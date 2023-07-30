package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.component.JwtProvider;
import com.devprofile.DevProfile.dto.ChatMessageDto;
import com.devprofile.DevProfile.dto.ChatRoomDto;
import com.devprofile.DevProfile.dto.UserEntityDto;
import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.entity.ChatMessage;
import com.devprofile.DevProfile.entity.ChatRoom;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.ChatMessageRepository;
import com.devprofile.DevProfile.repository.ChatRoomRepository;
import com.devprofile.DevProfile.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ChatApiController {

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final JwtProvider jwtProvider;

    public ChatApiController(UserRepository userRepository,
                             ChatRoomRepository chatRoomRepository,
                             ChatMessageRepository chatMessageRepository,
                             SimpMessagingTemplate messagingTemplate,
                             JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/chatroom")
    public ResponseEntity<ApiResponse<ChatRoomDto>> createChatRoom(@RequestParam String userName, @RequestHeader("Authorization") String authorization) {
        ApiResponse<ChatRoomDto> apiResponse = new ApiResponse<>();

        jwtProvider.validateToken(authorization);
        String primaryId = jwtProvider.getIdFromJWT(authorization);
        UserEntity user = userRepository.findById(Integer.parseInt(primaryId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        String currentUserName = user.getLogin();
        UserEntity send = userRepository.findByLogin(currentUserName);
        UserEntity receive = userRepository.findByLogin(userName);

        // 채팅방이 이미 존재하는지 확인
        ChatRoom existingChatRoom = chatRoomRepository.findBySendAndReceive(send, receive).orElse(null);
        if (existingChatRoom != null) {
            apiResponse.setResult(true);
            apiResponse.setData(transformChatRoomToDto(existingChatRoom));
            return ResponseEntity.ok(apiResponse);
        }

        // 채팅방 생성
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setSend(send);
        chatRoom.setReceive(receive);
        chatRoom.setCreatedAt(LocalDateTime.now());
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        apiResponse.setResult(true);
        apiResponse.setData(transformChatRoomToDto(savedChatRoom));
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/chatroom/{userName}/message")
    public ResponseEntity<ApiResponse<ChatMessageDto>> sendMessage(@PathVariable String userName, @RequestBody ChatMessage message, @RequestHeader("Authorization") String authorization) {
        ApiResponse<ChatMessageDto> apiResponse = new ApiResponse<>();

        jwtProvider.validateToken(authorization);
        String primaryId = jwtProvider.getIdFromJWT(authorization);
        UserEntity user = userRepository.findById(Integer.parseInt(primaryId)).orElseThrow(() -> new RuntimeException("User not found"));
        String currentUserName = user.getLogin();
        UserEntity send = userRepository.findByLogin(currentUserName);
        UserEntity receive = userRepository.findByLogin(userName);

        Optional<ChatRoom> chatRoomOptional = chatRoomRepository.findById(message.getChatRoom().getId());
        if (!chatRoomOptional.isPresent()) {
            apiResponse.setResult(false);
            apiResponse.setMessage("ChatRoom not found");
            return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
        }

        ChatRoom chatRoom = chatRoomOptional.get();
        message.setSender(send);
        message.setChatRoom(chatRoom);
        message.setTimestamp(LocalDateTime.now());
        ChatMessage savedMessage = chatMessageRepository.save(message);
        messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoom.getId(), savedMessage);

        ChatMessageDto chatMessageDto = new ChatMessageDto();
        chatMessageDto.setId(savedMessage.getId());
        chatMessageDto.setMessage(savedMessage.getMessage());
        chatMessageDto.setTimestamp(savedMessage.getTimestamp());

        UserEntityDto userEntityDto = new UserEntityDto();
        userEntityDto.setId(savedMessage.getSender().getId());
        userEntityDto.setLogin(savedMessage.getSender().getLogin());

        chatMessageDto.setSender(userEntityDto);

        apiResponse.setResult(true);
        apiResponse.setData(chatMessageDto);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/chatroom/{chatRoomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageDto>>> getChatRoomMessages(@PathVariable Long chatRoomId) {
        ApiResponse<List<ChatMessageDto>> apiResponse = new ApiResponse<>();

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found"));

        List<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderByTimestamp(chatRoom);

        List<ChatMessageDto> messagesDto = new ArrayList<>();
        for (ChatMessage message : messages) {
            ChatMessageDto messageDto = new ChatMessageDto();
            messageDto.setId(message.getId());
            messageDto.setMessage(message.getMessage());
            messageDto.setTimestamp(message.getTimestamp());

            UserEntityDto senderDto = new UserEntityDto();
            senderDto.setId(message.getSender().getId());
            senderDto.setLogin(message.getSender().getLogin());
            messageDto.setSender(senderDto);

            messagesDto.add(messageDto);
        }

        apiResponse.setResult(true);
        apiResponse.setData(messagesDto);
        return ResponseEntity.ok(apiResponse);
    }

    private ChatRoomDto transformChatRoomToDto(ChatRoom chatRoom) {
        ChatRoomDto chatRoomDto = new ChatRoomDto();
        chatRoomDto.setId(chatRoom.getId());
        chatRoomDto.setCreatedAt(chatRoom.getCreatedAt());

        UserEntityDto sendDto = new UserEntityDto();
        sendDto.setId(chatRoom.getSend().getId());
        sendDto.setLogin(chatRoom.getSend().getLogin());
        chatRoomDto.setSend(sendDto);

        UserEntityDto receiveDto = new UserEntityDto();
        receiveDto.setId(chatRoom.getReceive().getId());
        receiveDto.setLogin(chatRoom.getReceive().getLogin());
        chatRoomDto.setReceive(receiveDto);

        return chatRoomDto;
    }
}
