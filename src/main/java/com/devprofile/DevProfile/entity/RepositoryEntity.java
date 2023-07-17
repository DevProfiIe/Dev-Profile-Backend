package com.devprofile.DevProfile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name="user_repository")
public class RepositoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private Integer userId;

    @Column
    private String repositoryName;
    @Column
    private String userName;

    public RepositoryEntity(String repositoryName, Integer userId, String userName) {
        this.repositoryName = repositoryName;
        this.userId = userId;
        this.userName = userName;
    }


    public RepositoryEntity() {

    }

}
