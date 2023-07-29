package com.devprofile.DevProfile.dto.response;

import com.devprofile.DevProfile.entity.RepositoryEntity;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Data
public class RepositoryEntityDTO {
    private Long id;
    private String repoName;
    private String repoDesc;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalCommitCnt;
    private Integer myCommitCnt;
    private Integer totalContributors;
    private Set<String> repoLanguages;
    private List<String> featured;
    private List<String> langFramework;

}
