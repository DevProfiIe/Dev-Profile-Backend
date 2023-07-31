package com.devprofile.DevProfile.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class WordEntity {

    @Id
    private String word;

    private String realWord;
}
