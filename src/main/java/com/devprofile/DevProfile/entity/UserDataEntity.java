package com.devprofile.DevProfile.entity;


import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;


@Getter
@Setter
@Document(collection = "userData")
public class UserDataEntity {

    @Id
    private String userName;

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

    private Integer webFrontend;
    private Set<String> webFrontendSet;

    private Integer systemProgramming;
    private Set<String> systemProgrammingSet;

    private Integer game;
    private Set<String> gameSet;

    private Integer algorithm;
    private Set<String> algorithmSet;

    /*
    현재 개별 멤버로 관리 -> 연관정보 map(혹은 객체로) 처리 묶어서.
    테스트 결과, MongoDB 트랜잭션에서 객체 그대로 저장으로 인해 increase 대신 update로 덮어 씌워지는 문제
    잘못 만들면 count가 크게 어긋남. 우선 개별 멤버로 구현.

    map 형식 구현 구상(기존 작업하려던 모양)
    type		Map<String, Object>

    key			value
    -----		-----
    String		Integer
    count		6

    String		Set<String>
    featureSet	["str1", "str2", "str3", "str4"]


    document:{
    	count:6,
    	featureSet:["str1", "str2", "str3", "str4"]
    }

    */
}
