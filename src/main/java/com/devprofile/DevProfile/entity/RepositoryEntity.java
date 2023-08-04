package com.devprofile.DevProfile.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private LocalDate repoCreated;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate repoUpdated;
    private String repoDesc;
    private String repoUrl;
    private String orgName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
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

    private LocalDate convertToLocalDate(String dateStr) {
        Instant instant = Instant.parse(dateStr);
        LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        return date;
    }


}
