package com.devprofile.DevProfile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "user_info")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Integer userId;

    @Column
    private String userNodeId;

    @Column
    private String userName;

    @Column
    private String userNickName;
    @Column
    private String userImg;

    @Column
    private String userEmail;
}