package com.devprofile.DevProfile.dto.response.analyze;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@Data
public class UserPageDTO {
    private String userName;
    private List<String> language;
    private List<String> framework;
    private Set<String> styles;
    private String field;
    private String avatarUrl;
    private Integer repoCount;
    private Integer commitCount;
    private Integer commitDays;
    private Boolean selected = false;
}
