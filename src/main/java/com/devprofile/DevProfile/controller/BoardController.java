package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.dto.response.analyze.UserPageDTO;
import com.devprofile.DevProfile.entity.*;
import com.devprofile.DevProfile.repository.*;
import com.devprofile.DevProfile.service.FilterService;
import com.devprofile.DevProfile.service.search.SearchService;
import com.devprofile.DevProfile.service.search.SparqlService;
import com.devprofile.DevProfile.service.userData.UserDataService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@AllArgsConstructor
public class BoardController {

    private final UserDataRepository userDataRepository;
    private final UserRepository userRepository;
    private final GitRepository gitRepository;
    private final UserDataService userDataService;
    private final SearchService searchService;
    private final RepoFrameworkRepository repoFrameworkRepository;
    private final FrameworkRepository frameworkRepository;
    private final FilterService filterService;

    @GetMapping("/board")
    public ResponseEntity<ApiResponse> userBoardData(
            @RequestParam(required = false) List<String> languageFilters,
            @RequestParam(required = false) List<String> frameworkFilters,
            @RequestParam(required = false) Long languageDurationFilter,
            @RequestParam(required = false) Long frameworkDurationFilter,
            @RequestParam(required = false) List<String> keywordsFilter,
            @RequestParam(required = false) String field,
            @RequestParam(required = false) Integer fieldScore){


        ApiResponse<List<UserPageDTO>> apiResponse = new ApiResponse<>();
        ModelMapper modelMapper = new ModelMapper();

        List<UserPageDTO> userList = new ArrayList<>();
        List<UserEntity> UserEntityList = userRepository.findAll();

        for (UserEntity userEntity : UserEntityList) {
            boolean isLanguageMatched = languageFilters == null || languageFilters.isEmpty();
            boolean isFrameworkMatched = frameworkFilters == null || frameworkFilters.isEmpty();
            UserPageDTO userPageDTO = new UserPageDTO();
            UserDataEntity userDataEntity = userDataRepository.findByUserName(userEntity.getLogin());
            if (userDataEntity == null) continue;
            if (field != null && fieldScore != null) {
                Integer userFieldScore = null;
                switch (field) {
                    case "ai":
                        userFieldScore = userDataEntity.getAi();
                        break;
                    case "database":
                        userFieldScore = userDataEntity.getDatabase();
                        break;
                    case "webBackend":
                        userFieldScore = userDataEntity.getWebBackend();
                        break;
                    case "webFrontend":
                        userFieldScore = userDataEntity.getWebFrontend();
                        break;
                    case "game":
                        userFieldScore = userDataEntity.getGame();
                        break;
                    case "systemProgramming":
                        userFieldScore = userDataEntity.getSystemProgramming();
                        break;
                }
                if (userFieldScore == null || userFieldScore < fieldScore) continue;
                if (keywordsFilter != null && !userDataEntity.getKeywordSet().containsAll(keywordsFilter)) continue;
            }

            List<RepositoryEntity> repositoryEntities = gitRepository.findByUserId(userEntity.getId());
            Map<String, Long> langUsage = new HashMap<>();
            Map<String, Long> frameUsage = new HashMap<>();

            for (RepositoryEntity repositoryEntity : repositoryEntities) {
                List<LanguageDuration> languageDurations = repositoryEntity.getLanguageDurations();
                List<RepoFrameworkEntity> frameworkDurations = repoFrameworkRepository.findAllByRepoName(repositoryEntity.getRepoName());
                for (LanguageDuration languageDuration : languageDurations) {
                    if (languageFilters != null && languageFilters.contains(languageDuration.getLanguage())
                            && (languageDurationFilter == null || languageDuration.getDuration().longValue() >= languageDurationFilter)) {
                        isLanguageMatched = true;
                    }
                    String lang = languageDuration.getLanguage();
                    Long usage = languageDuration.getDuration().longValue();
                    Long currentValue = langUsage.getOrDefault(lang, 0L);
                    langUsage.put(lang, currentValue + usage);
                }
                for (RepoFrameworkEntity repoFramework : frameworkDurations) {
                    if (frameworkFilters != null && frameworkFilters.contains(repoFramework.getFramework())
                            && (frameworkDurationFilter == null || repoFramework.getRepoDuration() >= frameworkDurationFilter)) { // 여기를 수정
                        isFrameworkMatched = true;
                    }
                    String framework = repoFramework.getFramework();
                    String frameworkUrl = frameworkRepository.findByFrameworkName(framework).getFrameworkUrl();
                    Long usage = repoFramework.getRepoDuration();
                    Long currentValue = frameUsage.getOrDefault(frameworkUrl, 0L);
                    frameUsage.put(frameworkUrl, currentValue + usage);
                }
            }
            List<String> language = new ArrayList<>();
            List<String> framework = new ArrayList<>();
            langUsage.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .forEach(entry -> language.add(entry.getKey()));
            frameUsage.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .forEach(entry -> framework.add(entry.getKey()));
            if (isLanguageMatched && isFrameworkMatched) {
                userPageDTO.setLanguage(language);
                userPageDTO.setFramework(framework);
                userPageDTO.setUserName(userEntity.getName());
                userPageDTO.setField(userDataService.findMaxFieldInList(userDataEntity.getUserName()));
                userPageDTO.setAvatarUrl(userEntity.getAvatar_url());
                userList.add(userPageDTO);
            }
        }
        apiResponse.setMessage(null);
        apiResponse.setToken(null);
        apiResponse.setResult(true);
        apiResponse.setData(userList);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/autoKeyword")
    public ResponseEntity<ApiResponse<List<String>>> makeKeyword(@RequestParam String query){
        List<String> keywords = searchService.getCloseWords(query);
        ApiResponse<List<String>> apiResponse = new ApiResponse<>();
        apiResponse.setMessage(null);
        apiResponse.setToken(null);
        apiResponse.setResult(true);
        apiResponse.setData(keywords);
        return ResponseEntity.ok(apiResponse);
    }
}
