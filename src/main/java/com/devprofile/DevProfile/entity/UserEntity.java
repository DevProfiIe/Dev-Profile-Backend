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

    @Column
    private String name;

    @Column
    private String email;

    @Column
    private String node_id;

    @Column
    private String avatar_url;

    @Column
    private String jwtToken;

    @Column
    private String gitHubToken;
}