package com.devprofile.DevProfile.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "framework")
public class FrameworkEntity {

    @Id
    private Integer id;

    @Column
    private String framework_name;
}
