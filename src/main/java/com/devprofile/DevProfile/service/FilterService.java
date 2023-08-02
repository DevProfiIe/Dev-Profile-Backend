package com.devprofile.DevProfile.service;

import com.devprofile.DevProfile.entity.FilterEntity;
import com.devprofile.DevProfile.entity.LanguageDuration;
import com.devprofile.DevProfile.entity.RepoFrameworkEntity;
import com.devprofile.DevProfile.entity.UserDataEntity;
import com.devprofile.DevProfile.repository.FilterRepository;
import com.devprofile.DevProfile.repository.LanguageDurationRepository;
import com.devprofile.DevProfile.repository.RepoFrameworkRepository;
import com.devprofile.DevProfile.repository.UserDataRepository;
import lombok.AllArgsConstructor;
import org.hibernate.usertype.LoggableUserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class FilterService {
    private final FilterRepository filterRepository;
    private final RepoFrameworkRepository repoFrameworkRepository;
    private final LanguageDurationRepository languageDurationRepository;
    private final UserDataRepository userDataRepository;

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

    public List<UserDataEntity> keyWordFilter(List<String> keywords, List<UserDataEntity> userDataEntities){
        List<UserDataEntity> filteredUserData = new ArrayList<>();
        for(UserDataEntity userDataEntity : userDataEntities){
            if(userDataEntity.getKeywordSet().containsAll(keywords))filteredUserData.add(userDataEntity);
        }
        return filteredUserData;
    }
}

