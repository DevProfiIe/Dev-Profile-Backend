package com.devprofile.DevProfile.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "repo_framework")
public class RepoFrameworkEntity {
    @Id
    private String repoName;
    private String framework;

}
