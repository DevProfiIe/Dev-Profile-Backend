package com.devprofile.DevProfile.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name="style")
public class StyleEntity {

    @Id
    private Integer id;

    @Column
    private String style;
    private String styleDescription;
}
