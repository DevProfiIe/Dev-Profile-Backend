package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.entity.CommitKeywordsEntity;
import com.devprofile.DevProfile.entity.UserDataEntity;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.CommitKeywordsRepository;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.devprofile.DevProfile.repository.UserDataRepository;
import com.devprofile.DevProfile.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class BoardController {

    private final UserDataRepository userDataRepository;
    private final UserRepository userRepository;
    private final CommitKeywordsRepository commitKeywordsRepository;
    private final CommitRepository commitRepository;


    @GetMapping("/board")
    public ResponseEntity<ApiResponse> userBoardData() throws IllegalAccessException, NoSuchFieldException {
        ApiResponse<List<Map<String, Object>>> apiResponse = new ApiResponse<>();
        List<Map<String, Object>> resultList = new ArrayList<>();
        List<UserEntity> UserEntityList = userRepository.findAll();
        double maxScoreCount = 0;
        double maxScoreRecencyLength = 0;


        for(UserEntity userEntity : UserEntityList) {
            UserDataEntity userDataEntity = userDataRepository.findByUserName(userEntity.getLogin());
            if(userDataEntity == null) continue;

            Field[] fields = UserDataEntity.class.getDeclaredFields();
            for(Field field : fields){
                if(field.getType().equals(Integer.class) && !field.getName().endsWith("Set")){
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("field", field.getName());
                    resultMap.put("login", userEntity.getLogin());
                    resultMap.put("feature", getFeature(userDataEntity, field.getName()));
                    double rawScoreCount = calculateScoreCount(userDataEntity, field.getName());
                    double rawScoreRecencyLength = calculateScoreRecencyLength(userDataEntity, field.getName());
                    resultMap.put("rawScoreCount", rawScoreCount);
                    resultMap.put("rawScoreRecencyLength", rawScoreRecencyLength);
                    maxScoreCount = Math.max(maxScoreCount, rawScoreCount);
                    maxScoreRecencyLength = Math.max(maxScoreRecencyLength, rawScoreRecencyLength);
                    resultList.add(resultMap);
                }
            }
        }


        for (Map<String, Object> resultMap : resultList) {
            double rawScoreCount = (double) resultMap.get("rawScoreCount");
            double rawScoreRecencyLength = (double) resultMap.get("rawScoreRecencyLength");
            double normalizedScoreCount = (rawScoreCount / maxScoreCount) * 50.0;
            double normalizedScoreRecencyLength = (rawScoreRecencyLength / maxScoreRecencyLength) * 50.0;
            String login = (String) resultMap.get("login");
            System.out.println("normalizedScoreRecencyLength = " + normalizedScoreRecencyLength);
            System.out.println("normalizedScoreCount = " + normalizedScoreCount);
            int finalScore = (int) Math.round(normalizedScoreCount + normalizedScoreRecencyLength);
            resultMap.put(login, finalScore);
            resultMap.remove("rawScoreCount");
            resultMap.remove("rawScoreRecencyLength");
        }

        apiResponse.setMessage(null);
        apiResponse.setToken(null);
        apiResponse.setResult(true);
        apiResponse.setData(resultList);
        return ResponseEntity.ok(apiResponse);
    }

    private Set<String> getFeature(UserDataEntity userDataEntity, String field) throws IllegalAccessException, NoSuchFieldException {
        Field declaredField = UserDataEntity.class.getDeclaredField(field + "Set");
        declaredField.setAccessible(true);
        return (Set<String>) declaredField.get(userDataEntity);
    }

    private double calculateScoreCount(UserDataEntity userDataEntity, String field) throws IllegalAccessException, NoSuchFieldException {
        Field declaredField = UserDataEntity.class.getDeclaredField(field);
        declaredField.setAccessible(true);
        Integer count = (Integer) declaredField.get(userDataEntity);

        double countScore = Math.log1p(count) / Math.log1p(100) * 50;
        return countScore;
    }

    private double calculateScoreRecencyLength(UserDataEntity userDataEntity, String field) throws IllegalAccessException, NoSuchFieldException {
        Field declaredField = UserDataEntity.class.getDeclaredField(field);
        declaredField.setAccessible(true);
        Integer count = (Integer) declaredField.get(userDataEntity);

        List<CommitKeywordsEntity> commitKeywords = commitKeywordsRepository.findByFieldContaining(field);

        double recencyLengthScore = 0;
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        double maxCommitLength = 1000.0;

        for (CommitKeywordsEntity commitKeyword : commitKeywords) {
            String oid = commitKeyword.getOid();
            CommitEntity commitEntity = commitRepository.findByCommitOid(oid).orElse(null);
            if (commitEntity == null) continue;

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime commitDate = commitEntity.getCommitDate().atStartOfDay();

            if (commitDate.isAfter(oneYearAgo)) {
                long daysBetween = Duration.between(commitDate, now).toDays();
                double recencyRatio = (365 - daysBetween) / 365.0;

                double lengthRatio = commitEntity.getLength() / maxCommitLength;

                recencyLengthScore += recencyRatio * lengthRatio;
            }
        }

        return recencyLengthScore;
    }
}
