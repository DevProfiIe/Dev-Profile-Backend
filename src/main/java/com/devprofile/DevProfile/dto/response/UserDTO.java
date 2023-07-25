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

    private List<Map<String, Object>> commitCalender;

    private Set<String> keywordSet;

    private Integer ai;

    private Integer dataScience;

    private Integer database;

    private Integer mobile;

    private Integer webBackend;

    private Integer document;

    private Integer systemProgramming;

    private Integer algorithm;

    private Integer game;


}