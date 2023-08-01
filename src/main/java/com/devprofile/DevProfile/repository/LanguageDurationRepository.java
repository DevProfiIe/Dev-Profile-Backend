package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.LanguageDuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LanguageDurationRepository extends JpaRepository<LanguageDuration, Long> {
}
