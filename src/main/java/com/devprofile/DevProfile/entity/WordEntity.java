package com.devprofile.DevProfile.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name="search_dictionary", indexes = {@Index(name = "idx_first_char", columnList = "firstChar")})
public class WordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String queryWord;
    private String keyword;
    private char firstChar;
}
