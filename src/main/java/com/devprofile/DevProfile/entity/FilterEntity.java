package com.devprofile.DevProfile.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Document(collection = "filter")
@JsonIgnoreProperties({"_id"})
public class FilterEntity {
    @Id
    private String id;
    private String userLogin;
    private String userName;
    private String avatarUrl;
    private String field;
    private Set<String> styles;
    private Map<String,Integer> languages;
    private Map<String,Integer> frameworks;

}

