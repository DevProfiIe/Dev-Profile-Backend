package com.devprofile.DevProfile.service;


import com.devprofile.DevProfile.entity.FilterEntity;
import com.devprofile.DevProfile.entity.LanguageDuration;
import com.devprofile.DevProfile.entity.RepoFrameworkEntity;
import com.devprofile.DevProfile.repository.FilterRepository;
import com.devprofile.DevProfile.repository.LanguageDurationRepository;
import com.devprofile.DevProfile.repository.RepoFrameworkRepository;

import com.devprofile.DevProfile.dto.response.analyze.UserPageDTO;
import com.devprofile.DevProfile.entity.*;
import com.devprofile.DevProfile.repository.*;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class FilterService {
    private final FilterRepository filterRepository;
    private final RepoFrameworkRepository repoFrameworkRepository;
    private final LanguageDurationRepository languageDurationRepository;

    private final UserDataRepository userDataRepository;
    private final UserRepository userRepository;
    private final UserScoreRepository userScoreRepository;
    private final FrameworkRepository frameworkRepository;
    private final CommitRepository commitRepository;
    private final GitRepository gitRepository;

    public FilterEntity createAndSaveFilter(String userLogin) {
        FilterEntity existingFilter = filterRepository.findByUserLogin(userLogin);
        UserEntity user = userRepository.findByLogin(userLogin);
        UserDataEntity userData = userDataRepository.findByUserName(userLogin);

        if (existingFilter != null) {
            return existingFilter;
        }

        FilterEntity filterEntity = new FilterEntity();
        filterEntity.setUserName(user.getName());
        filterEntity.setUserLogin(userLogin);
        filterEntity.setStyles(userData.getUserStyle());

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

        List<CommitEntity> commitEntities= commitRepository.findByUserName(userLogin);
        Optional<LocalDate> maxDate = commitEntities.stream()
                .map(CommitEntity::getCommitDate)
                .max(LocalDate::compareTo);
        Optional<LocalDate> minDate = commitEntities.stream()
                .map(CommitEntity::getCommitDate)
                .min(LocalDate::compareTo);

        Integer commitDays = minDate.orElseThrow().compareTo(maxDate.orElseThrow());
        Integer commitCount = commitEntities.size();
        Integer repoCount = gitRepository.findByUserId(user.getId()).size();
        filterEntity.setCommitCount(commitCount);
        filterEntity.setRepoCount(repoCount);
        filterEntity.setCommitDays(commitDays);


        return filterRepository.save(filterEntity);
    }

    public UserPageDTO filterChangeToDTO(FilterEntity filter){
        UserPageDTO userPageDTO = new UserPageDTO();
        userPageDTO.setAvatarUrl(filter.getAvatarUrl());
        userPageDTO.setStyles(filter.getStyles());
        userPageDTO.setUserName(filter.getUserName());
        userPageDTO.setField(filter.getField());
        List<String> framework = new ArrayList<>(filter.getFrameworks().keySet());
        framework.sort(Comparator.comparingInt(filter.getFrameworks()::get).reversed());
        userPageDTO.setFramework(framework.size() > 3 ? framework.subList(0, 3) : framework);

        List<String> language = new ArrayList<>(filter.getLanguages().keySet());
        language.sort(Comparator.comparingInt(filter.getLanguages()::get).reversed());
        userPageDTO.setLanguage(language.size() > 3 ? language.subList(0, 3) : language);
        userPageDTO.setCommitCount(filter.getCommitCount());
        userPageDTO.setRepoCount(filter.getRepoCount());
        userPageDTO.setCommitDays(filter.getCommitDays());

        return userPageDTO;
    }

    public void makeMockupData(){
        List<FrameworkEntity> frameworkEntities = frameworkRepository.findAll();
        List<String> frameworkList = new ArrayList<>();
        for(FrameworkEntity framework: frameworkEntities){
            Map<String, String> frameworkMap = new HashMap<>();
            frameworkList.add(framework.getFrameworkName());
        }
        List<String> languageList = new ArrayList<>();
        languageList.add("Java");
        languageList.add("C");
        languageList.add("Ruby");
        languageList.add("C++");
        languageList.add("Python");
        languageList.add("Go");
        languageList.add("C#");
        languageList.add("JavaScript");
        languageList.add("TypeScript");
        languageList.add("PHP");
        languageList.add("Kotlin");
        languageList.add("Rust");
        languageList.add("R");
        languageList.add("Swift");
        languageList.add("Perl");

        List<String> styleList = new ArrayList<>();
        styleList.add("지속적인 개발자");
        styleList.add("싱글 플레이어");
        styleList.add("멀티 플레이어");
        styleList.add("1일 1커밋");
        styleList.add("\"리드미\" 리드미");
        styleList.add("인기 개발자");
        styleList.add("영향력 있는 개발자");
        styleList.add("새벽형 개발자");
        styleList.add("아침형 개발자");
        styleList.add("설명충");
        styleList.add("주말 커밋 전문가");
        styleList.add("주말 커밋자");
        styleList.add("실속없는 커밋자");

        List<String> userNameList = Arrays.asList(
                "JihoonKim",
                "YeonSooLee",
                "MinseokPark",
                "EunjiChoi",
                "HyunwooKang",
                "SoheeLim",
                "JungsooHan",
                "SeojinYoon",
                "YonghaJang",
                "MinjiKo",
                "SungminShin",
                "HaeunSong",
                "JunheeMoon",
                "YoonhoNa",
                "SaebyeokOh",
                "HaerinKwon",
                "JihoonJung",
                "SeungyeonHwang",
                "YejinKim",
                "SeongsooPark"
        );

    }
}

