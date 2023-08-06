package com.devprofile.DevProfile.dto.response.analyze;

import com.devprofile.DevProfile.entity.LanguageDuration;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Data
public class RepositoryEntityDTO {
    private Long id;
    private String repoName;
    private String repoDesc;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer totalCommitCnt;
    private Integer myCommitCnt;
    private Integer totalContributors;
    private List<String> repoLanguages;
    private List<Map<String, String>> frameworkUrls;

    public void setLanguageDurations(List<LanguageDuration> languageDurations) {
    }
}
