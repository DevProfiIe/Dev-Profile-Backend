package com.devprofile.DevProfile.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Getter
@Setter
@Document(collection="user_status")
public class UserStatusEntity{

    @Id
    private String id;

    private String sendUserLogin;
    private String receiveUserLogin;
    private String boardUserLogin;
    private String userStatus;
    private Boolean selectedStatus;

}
