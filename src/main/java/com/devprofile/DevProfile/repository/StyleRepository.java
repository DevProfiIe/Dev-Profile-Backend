package com.devprofile.DevProfile.repository;


import com.devprofile.DevProfile.entity.StyleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StyleRepository extends JpaRepository<StyleEntity, Integer> {


}
