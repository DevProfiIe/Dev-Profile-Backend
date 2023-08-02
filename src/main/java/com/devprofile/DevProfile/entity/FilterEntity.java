package com.devprofile.DevProfile.entity;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Getter
@Setter
@Document(collection = "filter")
public class FilterEntity {
    @Id
    private String id;
    private String username;
    private Map<String,Integer> languages;
    private Map<String,Integer> frameworks;

}
