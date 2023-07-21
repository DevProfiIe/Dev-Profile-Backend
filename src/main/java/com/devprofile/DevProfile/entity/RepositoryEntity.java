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
    private String repoName;

    @Column
    private String repoNodeId;

    @Column
    private String repoCreated;

    @Column
    private String repoUpdated;

    @Column
    private String repoDesc;
    @Column
    private String repoUrl;


    public RepositoryEntity(Integer userId, String repoName, String repoNodeId, String repoCreated, String repoUpdated, String repoDesc, String repoUrl) {
        this.userId = userId;
        this.repoName = repoName;
        this.repoNodeId = repoNodeId;
        this.repoCreated = repoCreated;
        this.repoUpdated = repoUpdated;
        this.repoDesc = repoDesc;
        this.repoUrl = repoUrl;
    }

    public RepositoryEntity() {

    }

}