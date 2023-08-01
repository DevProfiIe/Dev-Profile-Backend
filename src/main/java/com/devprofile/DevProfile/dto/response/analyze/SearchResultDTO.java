package com.devprofile.DevProfile.dto.response.analyze;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;


@Getter
@Setter
@Data
public class SearchResultDTO {
    private String commitMessage;
    private LocalDate commitDate;
    private String keywordSet;
    private String repoName;
    private String commitOid;
}
