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

    @Column(name = "duration")
    private Integer duration;

    public LanguageDuration(String language, Integer duration) {
        this.language = language;
        this.duration = duration;
    }

    public LanguageDuration() {

    }
}
