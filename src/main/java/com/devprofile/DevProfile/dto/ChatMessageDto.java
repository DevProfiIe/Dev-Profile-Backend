package com.devprofile.DevProfile.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageDto {
    private Long id;
    private String message;
    private LocalDateTime timestamp;
    private UserEntityDto sender;
    private Long chatRoomId;
}
