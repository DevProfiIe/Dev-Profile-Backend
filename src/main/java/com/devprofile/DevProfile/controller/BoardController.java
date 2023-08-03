package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.dto.response.analyze.UserPageDTO;
import com.devprofile.DevProfile.entity.FilterEntity;
import com.devprofile.DevProfile.entity.UserDataEntity;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.entity.UserScore;
import com.devprofile.DevProfile.repository.*;
import com.devprofile.DevProfile.service.search.SearchService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
@AllArgsConstructor
public class BoardController {

    private final UserDataRepository userDataRepository;
    private final UserRepository userRepository;
    private final GitRepository gitRepository;
    private final FilterRepository filterRepository;
    private final SearchService searchService;
    private final UserScoreRepository userScoreRepository;

    @GetMapping("/board")
    public ResponseEntity<ApiResponse> userBoardData(
            @RequestParam(required = false) List<String> lang,
            @RequestParam(required = false) List<String> frame,
            @RequestParam(required = false) Long langDuration,
            @RequestParam(required = false) Long frameDuration,
            @RequestParam(required = false) List<String> keywordsFilter,
            @RequestParam(required = false) String field,
            @RequestParam(required = false) Integer fieldScore) {

        ApiResponse<List<UserPageDTO>> apiResponse = new ApiResponse<>();
        List<UserPageDTO> userList = new ArrayList<>();

        List<UserEntity> allUsers = userRepository.findAll();

        for (UserEntity userEntity : allUsers) {
            List<UserScore> userScores = userScoreRepository.findByLogin(userEntity.getLogin());

            if (userScores != null && !userScores.isEmpty()) {
                Optional<UserScore> userScore = userScores.stream()
                        .filter(score -> score.getField().equals(field))
                        .findFirst();

                UserPageDTO userPageDTO = new UserPageDTO();
                UserDataEntity userDataEntity = userDataRepository.findByUserName(userEntity.getLogin());


                int userFieldScore = userScore.map(UserScore::getScore).orElse(0);

                boolean isLanguageMatched = lang == null || lang.isEmpty();
                boolean isFrameworkMatched = frame == null || frame.isEmpty();

                if (fieldScore != null) {
                    if (userFieldScore < fieldScore || (keywordsFilter != null && !userDataEntity.getKeywordSet().containsAll(keywordsFilter))) {
                        return ResponseEntity.ok(apiResponse);
                    }
                }

                Map<String, Long> langUsage = new HashMap<>();
                Map<String, Long> frameUsage = new HashMap<>();

                FilterEntity filterEntity = filterRepository.findByUsername(userEntity.getLogin());
                if (filterEntity != null) {
                    Map<String, Integer> languages = filterEntity.getLanguages();
                    Map<String, Integer> frameworks = filterEntity.getFrameworks();

                    for (Map.Entry<String, Integer> languageEntry : languages.entrySet()) {
                        String langKey = languageEntry.getKey();
                        Integer langValue = languageEntry.getValue() / 30;

                        if (lang != null && lang.contains(langKey) && (langValue >= (langDuration != null ? langDuration : 0))) {
                            isLanguageMatched = true;
                        }

                        Long currentValue = langUsage.getOrDefault(langKey, 0L);
                        langUsage.put(langKey, currentValue + langValue);
                    }

                    for (Map.Entry<String, Integer> frameworkEntry : frameworks.entrySet()) {
                        String frameKey = frameworkEntry.getKey();
                        Integer frameValue = frameworkEntry.getValue() / 30;

                        if (frame != null && frame.contains(frameKey) && (frameValue >= (frameDuration != null ? frameDuration : 0))) {
                            isFrameworkMatched = true;
                        }

                        Long currentValue = frameUsage.getOrDefault(frameKey, 0L);
                        frameUsage.put(frameKey, currentValue + frameValue);
                    }
                }

                UserScore maxScoreUserScore = userScores.stream()
                        .max(Comparator.comparing(UserScore::getScore))
                        .orElse(null);

                if (maxScoreUserScore == null) {
                    return ResponseEntity.ok(apiResponse);
                }

                String maxField = maxScoreUserScore.getField();

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
                    userPageDTO.setField(maxField);
                    userPageDTO.setAvatarUrl(userEntity.getAvatar_url());
                    userList.add(userPageDTO);
                }
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
