package com.devprofile.DevProfile.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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
    private String commitOid;
    private String commitMessage;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate commitDate;
    private String userName;
    private String repoNodeId;
    private String repoName;
    private Integer length;


    public void setCommitDate(String commitDateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        ZonedDateTime date = ZonedDateTime.parse(commitDateStr, formatter);
        this.commitDate = date.toLocalDate();
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