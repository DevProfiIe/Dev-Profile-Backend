package com.devprofile.DevProfile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "repo_language")
public class LanguageDuration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "language")
    private String language;
    @Column
    private String login;

    @Column(name = "duration")
    private Integer duration;

    public LanguageDuration(String language, Integer duration, String login) {
        this.language = language;
        this.duration = duration;
        this.login = login;
    }

    public LanguageDuration() {

    }
}
