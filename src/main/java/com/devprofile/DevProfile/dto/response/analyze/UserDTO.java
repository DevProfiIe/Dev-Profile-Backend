package com.devprofile.DevProfile.dto.response.analyze;

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

    private Integer database;

    private Integer webBackend;

    private Integer webFrontend;
    private Integer game;

    private Integer systemProgramming;


}
