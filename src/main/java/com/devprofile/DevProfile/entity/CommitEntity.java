package com.devprofile.DevProfile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

@Entity
@Getter
@Setter
@Table(name="user_commit")
public class CommitEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private Integer userId;

    @Column
    private String commitMessage;

    @Column
    private String commitDate;

    @Column
    private String userName;

    @Column
    private String commitSha;

    @Column
    private String repoNodeId;
    @Column
    private String repoName;

    @Column
    private String language;


    public CommitEntity(Integer userId, String commitMessage, String commitDate, String userName, String commitSha) {
        this.userId = userId;
        this.commitMessage = commitMessage;
        this.commitDate = commitDate;
        this.userName = userName;
        this.commitSha = commitSha;
    }

    public CommitEntity() {

    }
}