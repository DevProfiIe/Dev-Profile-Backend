package com.devprofile.DevProfile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name="user_patch")
public class PatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String commitOid;
    private String keyword;
    private String fileName;

    @Column(columnDefinition="LONGTEXT")
    private String rawUrl;
    @Column(columnDefinition="LONGTEXT")
    private String contentsUrl;
    @Column(columnDefinition="LONGTEXT")
    private String patch;


}
