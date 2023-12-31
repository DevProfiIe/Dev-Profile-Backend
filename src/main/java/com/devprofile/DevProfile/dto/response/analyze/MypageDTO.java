package com.devprofile.DevProfile.dto.response.analyze;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Data
public class MypageDTO {
    private String id;
    private String userName;
    private List<String> filter;
    private Integer people;
    private String state;
    private String sendDate;
}
