package com.devprofile.DevProfile.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Data
@Document(collection = "list_filter")
public class ListEntity {

    @Id
    private String id;

    @Column
    private String sendUserLogin;
    private String receiveUserLogin;
    private Integer people;
    private Boolean status;
    private List<String> filter;
    private List<String> filteredNameList;
}
