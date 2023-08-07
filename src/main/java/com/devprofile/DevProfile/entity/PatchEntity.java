package com.devprofile.DevProfile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
    private String fileName;

    @Column(columnDefinition="LONGTEXT")
    private String contentsUrl;
    @Column(columnDefinition="LONGTEXT")
    private String patch;

    @Column
    private Integer additions;
    private Integer deletions;

    @Column
    private String userName;


}
