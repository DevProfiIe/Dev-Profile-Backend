package com.devprofile.DevProfile.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "repo_framework")
public class RepoFrameworkEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String repoName;
    private String framework;
    private String login;
    private Long repoDuration;
    private Long repoId;

    public void setRepoDuration(Long repoDuration) {
        this.repoDuration = repoDuration;
    }

}
