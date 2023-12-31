package com.devprofile.DevProfile.entity;


import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;
import java.util.Set;


@Getter
@Setter
@Document(collection = "userData")
public class UserDataEntity {

    @Id
    private String id;

    private String userName;

    private Set<String> keywordSet;

    private Integer ai;

    private Set<String> aiSet;

    private Integer database;

    private Set<String> databaseSet;

    private Integer webBackend;

    private Set<String> webBackendSet;

    private Integer webFrontend;

    private Set<String> webFrontendSet;

    private Integer game;

    private Set<String> gameSet;

    private Integer systemProgramming;

    private Set<String> systemProgrammingSet;

    private Set<String> userStyle;

    private String userKeywordAnalyze;

    private String userTitle;

    private Map<String, Integer> cs;
}