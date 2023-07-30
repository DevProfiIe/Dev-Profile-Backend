package com.devprofile.DevProfile.dto.response;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Data
public class UserPageDTO {
    private String userName;
    private List<String> techStack;
    private String field;
}
