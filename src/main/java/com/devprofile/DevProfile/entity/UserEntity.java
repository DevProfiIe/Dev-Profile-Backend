package com.devprofile.DevProfile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Entity
@Getter
@Setter
@Table(name = "user_info")
public class UserEntity {

    @Id
    private Integer id;

    @Column
    private String login;
    private String name;
    private String email;
    private String node_id;
    private String avatar_url;
    private String jwtRefreshToken;
    private String gitHubToken;
}