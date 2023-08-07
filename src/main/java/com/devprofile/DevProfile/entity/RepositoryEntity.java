package com.devprofile.DevProfile.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime repoCreated;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime repoUpdated;
    private String repoDesc;
    private String repoUrl;
    private String orgName;
    private String userName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime startDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime endDate;
    private Integer userId;
    private Integer totalCommitCnt;
    private Integer myCommitCnt;
    private Integer totalContributors;


    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "repoNodeId")
    private List<LanguageDuration> languageDurations = new ArrayList<>();

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
        this.repoCreated = convertToLocalDateTime(repoCreated);
    }

    public void setRepoUpdated(String repoUpdated) {
        this.repoUpdated = convertToLocalDateTime(repoUpdated);
    }

    private LocalDateTime convertToLocalDateTime(String dateTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formatter);
        return dateTime;
    }


}
