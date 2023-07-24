package com.devprofile.DevProfile.entity;


import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;


@Getter
@Setter
@Document(collection = "commitKeyword")
public class CommitKeywordsEntity {

    @Id
    private String oid;

    private Set<String> langFramework;

    private Set<String> cs;

    private Set<String> featured;


}
