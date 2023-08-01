package com.devprofile.DevProfile.dto.response.chat;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class ChatRoomDto {
    private Long id;
    private UserEntityDto send;
    private UserEntityDto receive;
    private String createdAt;

    public void setCreatedAt(LocalDateTime createdAt) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        this.createdAt = createdAt.format(formatter);
    }
}
