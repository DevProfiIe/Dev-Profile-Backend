package com.devprofile.DevProfile.dto;

import lombok.Data;

@Data
public class ChatMessageDto {
    private String from;
    private String text;
    private String chatRoomId;
    private String senderId;
}
