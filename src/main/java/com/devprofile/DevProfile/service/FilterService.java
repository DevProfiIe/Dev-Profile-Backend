package com.devprofile.DevProfile.service;

import com.devprofile.DevProfile.entity.FilterEntity;
import com.devprofile.DevProfile.entity.LanguageDuration;
import com.devprofile.DevProfile.entity.RepoFrameworkEntity;
import com.devprofile.DevProfile.repository.FilterRepository;
import com.devprofile.DevProfile.repository.LanguageDurationRepository;
import com.devprofile.DevProfile.repository.RepoFrameworkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FilterService {
    private final FilterRepository filterRepository;
    private final RepoFrameworkRepository repoFrameworkRepository;
    private final LanguageDurationRepository languageDurationRepository;

    @Autowired
    public FilterService(FilterRepository filterRepository, RepoFrameworkRepository repoFrameworkRepository, LanguageDurationRepository languageDurationRepository) {
        this.filterRepository = filterRepository;
        this.repoFrameworkRepository = repoFrameworkRepository;
        this.languageDurationRepository = languageDurationRepository;
    }

    public FilterEntity createAndSaveFilter(String username) {
        FilterEntity filterEntity = new FilterEntity();
        filterEntity.setUsername(username);

        List<RepoFrameworkEntity> repoFrameworkEntities = repoFrameworkRepository.findAll();
        Map<String, Integer> frameworks = repoFrameworkEntities.stream()
                .collect(Collectors.toMap(RepoFrameworkEntity::getFramework,
                        entity -> entity.getRepoDuration().intValue(),
                        Integer::sum));
        filterEntity.setFrameworks(frameworks);

        List<LanguageDuration> languageDurations = languageDurationRepository.findAll();
        Map<String, Integer> languages = languageDurations.stream()
                .collect(Collectors.toMap(LanguageDuration::getLanguage,
                        LanguageDuration::getDuration,
                        Integer::sum));
        filterEntity.setLanguages(languages);

        return filterRepository.save(filterEntity);
    }
}

