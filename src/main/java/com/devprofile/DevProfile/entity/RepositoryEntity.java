package com.devprofile.DevProfile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private LocalDate repoCreated;

    @Column
    private LocalDate repoUpdated;

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