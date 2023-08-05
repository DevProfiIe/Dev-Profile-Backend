package com.devprofile.DevProfile.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Getter
@Setter
@Document(collection="user_status")
public class UserStatusEntity{

    @Id
    private Long id;

    @Column
    private String sendUserLogin;
    private String receiveUserLogin;
    private String boardUserLogin;
    private String userStatus;
    private String selectedStatus;

}
