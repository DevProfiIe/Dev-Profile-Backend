package com.devprofile.DevProfile.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatRoomDto {
    private Long id;
    private UserEntityDto send;
    private UserEntityDto receive;
    private LocalDateTime createdAt;
}
