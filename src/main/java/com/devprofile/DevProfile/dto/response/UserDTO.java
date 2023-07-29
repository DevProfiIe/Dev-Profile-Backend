package com.devprofile.DevProfile.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class UserDTO {
    private String avatar_url;
    private String login;
    private String name;

    private LocalDate commitStart;

    private LocalDate commitEnd;

    private List<Map<String, Object>> commitCalender;

    private Set<String> keywordSet;

    private Integer ai;
    private Set<String> aiSet;

    private Integer dataScience;
    private Set<String> dataScienceSet;

    private Integer database;
    private Set<String> databaseSet;

    private Integer mobile;
    private Set<String> mobileSet;

    private Integer webBackend;
    private Set<String> webBackendSet;

    private Integer systemProgramming;
    private Set<String> systemProgrammingSet;

    private Integer algorithm;
    private Set<String> algorithmSet;

    private Integer game;
    private Set<String> gameSet;


}
