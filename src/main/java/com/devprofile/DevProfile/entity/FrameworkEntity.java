package com.devprofile.DevProfile.entity;

<<<<<<< HEAD
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
=======

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
>>>>>>> 0961806cd71ed900a77a8be1a6426f00e2c2fb48
@Table(name = "framework")
public class FrameworkEntity {

    @Id
<<<<<<< HEAD
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String frameworkName;
    private String frameworkUrl;

=======
    private Integer id;

    @Column
    private String framework_name;
>>>>>>> 0961806cd71ed900a77a8be1a6426f00e2c2fb48
}
