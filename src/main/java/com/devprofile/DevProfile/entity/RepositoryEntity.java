package com.devprofile.DevProfile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

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

    @Column
    private String orgName;

    @ElementCollection
    @CollectionTable(name = "repo_language", joinColumns = @JoinColumn(name = "repoNodeId"))
    @Column(name = "language")
    private Set<String> repoLanguages = new HashSet<>();

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