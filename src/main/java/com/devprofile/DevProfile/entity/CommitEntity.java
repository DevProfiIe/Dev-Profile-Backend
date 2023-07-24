package com.devprofile.DevProfile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private String commitOid;
    @Column
    private String commitMessage;

    @Column
    private LocalDate commitDate;

    @Column
    private String userName;

    @Column
    private String repoNodeId;

    @Column
    private String repoName;


    public void setCommitDate(String commitDateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime dateTime = LocalDateTime.parse(commitDateStr, formatter);
        this.commitDate = dateTime.toLocalDate();
    }
    public CommitEntity(Integer userId, String commitMessage, String commitDate, String userName, String commitOid) {
        this.userId = userId;
        this.commitMessage = commitMessage;
        this.setCommitDate(commitDate);
        this.userName = userName;
        this.commitOid = commitOid;
    }

    public CommitEntity() {

    }
}