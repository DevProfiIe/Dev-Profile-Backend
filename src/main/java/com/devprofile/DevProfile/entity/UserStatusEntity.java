package com.devprofile.DevProfile.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Getter
@Setter
@Table(name="user_status")
public class UserStatusEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sendUserLogin;
    private String receiveUserLogin;
    private String boardUserLogin;
    private String userStatus;
    private Boolean selectedStatus;

}
