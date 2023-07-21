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
    private Long userId;

    @Column
    private String repoName;
    @Column
    private String userName;

    @Column
    private Integer repoId;

    @Column
    private String repoNodeId;
    public RepositoryEntity(String repoName, Long userId, String userName,Integer repoId,String repoNodeId) {
        this.repoName = repoName;
        this.userId = userId;
        this.userName = userName;
        this.repoId = repoId;
        this.repoNodeId = repoNodeId;
    }


    public RepositoryEntity() {

    }

}