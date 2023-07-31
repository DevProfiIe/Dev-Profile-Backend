package com.devprofile.DevProfile.dto.response.chat;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class ChatMessageDto {
    private Long id;
    private String message;
    private String timestamp;
    private UserEntityDto sender;
    private Long chatRoomId;

    public void setTimestamp(LocalDateTime timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        this.timestamp = timestamp.format(formatter);
    }
}
