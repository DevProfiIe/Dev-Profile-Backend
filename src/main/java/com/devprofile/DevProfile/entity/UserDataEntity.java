package com.devprofile.DevProfile.entity;


import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Set;


@Getter
@Setter
@Document(collection = "userData")
public class UserDataEntity {

    @Id
    private String userName;

    private Set<String> keywordSet;
}