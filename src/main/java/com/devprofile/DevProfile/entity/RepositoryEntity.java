package com.devprofile.DevProfile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name="user_repository")
public class RepositoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String repoName;
    private String repoNodeId;
    private LocalDate repoCreated;
    private LocalDate repoUpdated;
    private String repoDesc;
    private String repoUrl;
    private String orgName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer userId;
    private Integer totalCommitCnt;
    private Integer myCommitCnt;
    private Integer totalContributors;


    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "repoNodeId")
    private List<LanguageDuration> languageDurations;

    public RepositoryEntity(Integer userId, String repoName, String repoNodeId, String repoCreated, String repoUpdated, String repoDesc, String repoUrl) {
        this.userId = userId;
        this.repoName = repoName;
        this.repoNodeId = repoNodeId;
        this.setRepoCreated(repoCreated);
        this.setRepoUpdated(repoUpdated);
        this.repoDesc = repoDesc;
        this.repoUrl = repoUrl;
    }

    public RepositoryEntity() {

    }
    public void setRepoCreated(String repoCreated) {
        this.repoCreated = convertToLocalDate(repoCreated);
    }

    public void setRepoUpdated(String repoUpdated) {
        this.repoUpdated = convertToLocalDate(repoUpdated);
    }

    private LocalDate convertToLocalDate(String dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);
        return localDateTime.toLocalDate();
    }

}