package com.devprofile.DevProfile.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

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

    public UserEntity() {

    }

    public UserEntity(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            UserEntity userEntity = objectMapper.readValue(jsonString, UserEntity.class);
            this.id = userEntity.getId();
            this.login = userEntity.getLogin();
            this.name = userEntity.getName();
            this.email = userEntity.getEmail();
            this.node_id = userEntity.getNode_id();
            this.avatar_url = userEntity.getAvatar_url();
            this.jwtRefreshToken = userEntity.getJwtRefreshToken();
            this.gitHubToken = userEntity.getGitHubToken();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
