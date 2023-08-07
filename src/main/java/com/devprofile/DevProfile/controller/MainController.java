package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.component.JwtProvider;
import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.dto.response.analyze.CommitKeywordsDTO;
import com.devprofile.DevProfile.dto.response.analyze.UserDTO;
import com.devprofile.DevProfile.entity.*;
import com.devprofile.DevProfile.repository.*;
import com.devprofile.DevProfile.service.FilterService;
import com.devprofile.DevProfile.service.RepositoryService;
import com.devprofile.DevProfile.service.ResponseService;
import com.devprofile.DevProfile.service.gpt.GptCommitService;
import com.devprofile.DevProfile.service.gpt.GptPatchService;
import com.devprofile.DevProfile.service.graphql.GraphOrgService;
import com.devprofile.DevProfile.service.graphql.GraphUserService;
import com.devprofile.DevProfile.service.rabbitmq.MessageOrgSenderService;
import com.devprofile.DevProfile.service.rabbitmq.MessageSenderService;
import com.devprofile.DevProfile.service.search.SparqlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final GraphUserService userService;
    private final GraphOrgService orgService;
    private final UserDataRepository userDataRepository;
    private final PatchRepository patchRepository;
    private final CommitRepository commitRepository;
    private final ResponseService responseService;
    private final GitRepository gitRepository;
    private final GptPatchService gptPatchService;
    private final GptCommitService gptCommitService;
    private final RepositoryService repositoryService;
    private final SparqlService sparqlService;
    private final CommitKeywordsRepository commitKeywordsRepository;
    private final FilterService filterService;
    private final UserScoreRepository userScoreRepository;
    private final MessageSenderService messageSenderService;
    private final MessageOrgSenderService messageOrgSenderService;


    private UserDTO convertToDTO(UserEntity userEntity, UserDataEntity userDataEntity) {
        UserDTO userDTO = new UserDTO();
        userDTO.setAvatar_url(userEntity.getAvatar_url());
        userDTO.setLogin(userEntity.getLogin());
        userDTO.setName(userEntity.getName());

        if (userDataEntity != null) {
            userDTO.setKeywordSet(userDataEntity.getKeywordSet());
            userDTO.setAi(userDataEntity.getAi());
            userDTO.setDatabase(userDataEntity.getDatabase());
            userDTO.setWebBackend(userDataEntity.getWebBackend());
            userDTO.setSystemProgramming(userDataEntity.getSystemProgramming());
            userDTO.setWebFrontend(userDataEntity.getWebFrontend());
            userDTO.setGame(userDataEntity.getGame());
        }

        return userDTO;
    }


    @GetMapping("/main")
    public Mono<Void> main(@RequestHeader String Authorization) throws IOException {
        jwtProvider.validateToken(Authorization);
        String primaryId = jwtProvider.getIdFromJWT(Authorization);
        log.info(primaryId);

        if (primaryId != null && !primaryId.isEmpty()) {
            UserEntity user = userRepository.findById(Integer.parseInt(primaryId)).orElseThrow();
            messageSenderService.MainSendMessage(user);
            messageOrgSenderService.orgMainSendMessage(user);

            Mono<Void> userRepos = userService.userOwnedRepositories(user);

            Mono<Void> orgRepos = orgService.orgOwnedRepositories(user);

            return Flux.merge(userRepos, orgRepos)
                    .then();
        } else {
            throw new IllegalArgumentException("The primaryId is null or empty");
        }
    }

    @GetMapping("/chat2")
    public String chat() {
        return "chat2";
    }



    @GetMapping("/user/keyword")
    public ResponseEntity<ApiResponse> giveKeywords(@RequestParam String userName) {
        ApiResponse<Set<String>> apiResponse = new ApiResponse<>();
        apiResponse.setResult(true);
        apiResponse.setData(userDataRepository.findByUserName(userName).getKeywordSet());
        apiResponse.setMessage(null);
        apiResponse.setToken(null);
        return ResponseEntity.ok(apiResponse);
    }




    @PostMapping("/test/gpt")
    public String testGpt(@RequestParam String userName) {
        gptPatchService.processAllEntities(userName);
        return "index";
    }


    @PostMapping("/test/gpt/score")
    public String testGptScore(@RequestParam String userName, @RequestParam String commitOid) {
        gptCommitService.processOneCommit(userName, commitOid);
        return "index";
    }


    @PostMapping("/test")
    public String test(@RequestParam String userName) {
        filterService.createAndSaveFilter(userName);
        List<PatchEntity> patchEntities = patchRepository.findByCommitOid("d394b3b52e18f89773ed02af5163d91d62cfd797");
        patchEntities.forEach(patchEntity -> gptPatchService.generateKeyword(userName, patchEntity));

        return "index";
    }



    @GetMapping("/response_test")
    public ResponseEntity<ApiResponse<Object>> responseApiTest(@RequestParam String userName) {
        List<CommitEntity> allCommitEntities = commitRepository.findByUserName(userName);
        Map<String, CommitKeywordsDTO> oidAndKeywordsMap = new HashMap<>();
        Map<LocalDate, Integer> calendar = new HashMap<>();

        for (CommitEntity commitEntity : allCommitEntities) {
            Optional<CommitKeywordsDTO> keywords = responseService.getFeatureFramework(commitEntity.getCommitOid(), commitEntity.getUserId());
            if (keywords.isPresent()) {
                oidAndKeywordsMap.put(commitEntity.getCommitOid(), keywords.get());
            }
            LocalDate day = commitEntity.getCommitDate();
            calendar.merge(day, 1, Integer::sum);
        }
        List<Map<String, Object>> calendarData = new ArrayList<>();
        LocalDate firstDate = LocalDate.now();
        LocalDate lastDate = LocalDate.MIN;
        for (LocalDate day : calendar.keySet()) {
            if (firstDate.isAfter(day)) firstDate = day;
            if (lastDate.isBefore(day)) lastDate = day;
            Map<String, Object> oneDay = new HashMap<>();
            oneDay.put("day", day);
            oneDay.put("value", calendar.get(day));
            calendarData.add(oneDay);
        }

        repositoryService.saveFrameworksToNewTable(allCommitEntities, oidAndKeywordsMap,userName);
        List<CommitEntity> filteredCommitEntities = allCommitEntities.stream()
                .filter(commitEntity -> commitEntity.getUserName().equals(userName))
                .collect(Collectors.toList());

        List<RepositoryEntity> repositoryEntities = gitRepository.findWithCommitAndEndDate();
        List<RepositoryEntity> filteredRepositoryEntities = repositoryEntities.stream()
                .filter(repositoryEntity -> repositoryEntity.getUserName().equals(userName))
                .collect(Collectors.toList());

        Map<String, CommitKeywordsDTO> filteredOidAndKeywordsMap = oidAndKeywordsMap.entrySet().stream()
                .filter(entry -> filteredCommitEntities.stream().anyMatch(commitEntity -> commitEntity.getCommitOid().equals(entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        UserEntity userEntity = userRepository.findByLogin(userName);
        UserDataEntity userDataEntity = userDataRepository.findByUserName(userName);
        UserDTO userDTO = null;
        if (userEntity != null) {
            userDTO = convertToDTO(userEntity, userDataEntity);
            userDTO.setCommitCalender(calendarData);
            userDTO.setCommitStart(firstDate);
            userDTO.setCommitEnd(lastDate);
        }

        String message = null;
        if (userDTO == null) {
            message = "Fail: No user found";
        } else if (filteredRepositoryEntities.isEmpty()) {
            message = "Fail: No data found";
        }

        Map<String, Object> combinedData = new HashMap<>();
        combinedData.put("userInfo", userDTO);
        combinedData.put("repositoryInfo", repositoryService.createRepositoryEntityDTOs(filteredRepositoryEntities, filteredCommitEntities, filteredOidAndKeywordsMap));

        ApiResponse<Object> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userDTO != null && !filteredRepositoryEntities.isEmpty());
        apiResponse.setData(combinedData);
        apiResponse.setMessage(message);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/combined_data")
    public ResponseEntity<ApiResponse<Object>> combinedData(@RequestParam String userName) {
        ApiResponse<Object> apiResponse = new ApiResponse<>();
        Map<String, Object> combinedData = new HashMap<>();

        ResponseEntity<ApiResponse> userBoardResponse;
        try {
            userBoardResponse = userBoardData(userName);
            if (userBoardResponse.getBody() != null && userBoardResponse.getBody().isResult()) {
                combinedData.put("boardData", userBoardResponse.getBody().getData());
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
            apiResponse.setResult(false);
            apiResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.ok(apiResponse);
        }


        ResponseEntity<ApiResponse<Object>> responseTest = responseApiTest(userName);
        if (responseTest.getBody() != null && responseTest.getBody().isResult()) {
            combinedData.putAll((Map<? extends String, ?>) responseTest.getBody().getData());
        }

        apiResponse.setResult(true);
        apiResponse.setData(combinedData);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/user_keyword")
    public ResponseEntity<ApiResponse> user_keyword(@RequestParam String userName){
        ApiResponse<UserDataEntity> apiResponse = new ApiResponse<>();
        apiResponse.setToken(null);
        apiResponse.setResult(true);
        apiResponse.setData(userDataRepository.findByUserName(userName));
        apiResponse.setMessage(null);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/test")
    public ResponseEntity<String> test(){
        sparqlService.sparqlEntity();
        return ResponseEntity.ok(" ");
    }




    public ResponseEntity<ApiResponse> userBoardData(String userName) throws IllegalAccessException, NoSuchFieldException {
        ApiResponse<List<Map<String, Object>>> apiResponse = new ApiResponse<>();
        List<Map<String, Object>> resultList = new ArrayList<>();
        List<UserEntity> userEntityList = userRepository.findAll();
        double maxScoreCount = 0;
        double maxScoreRecencyLength = 0;


        for (UserEntity userEntity : userEntityList) {
            if (!userEntity.getLogin().equals(userName)) {
                continue;
            }

            UserDataEntity userDataEntity = userDataRepository.findByUserName(userEntity.getLogin());
            if (userDataEntity == null) {
                continue;
            }

            Field[] fields = UserDataEntity.class.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType().equals(Integer.class) && !field.getName().endsWith("Set")) {
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
            String field = (String) resultMap.get("field");
            int finalScore = (int) Math.round(normalizedScoreCount + normalizedScoreRecencyLength);

            UserScore existingUserScore = userScoreRepository.findByFieldAndLogin(field, login);
            if (existingUserScore != null) {
                existingUserScore.setScore(finalScore);
                userScoreRepository.save(existingUserScore);
            } else {
                UserScore newUserScore = new UserScore();
                newUserScore.setField(field);
                newUserScore.setLogin(login);
                newUserScore.setScore(finalScore);
                userScoreRepository.save(newUserScore);
            }

            Map<String, Object> userLogin = new HashMap<>();
            userLogin.put("login", login);
            userLogin.put("score", finalScore);

            resultMap.put("userlogin", userLogin);
            resultMap.remove("rawScoreCount");
            resultMap.remove("rawScoreRecencyLength");
            resultMap.remove("login");
            resultMap.remove(login);
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
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        double maxCommitLength = 1000.0;

        for (CommitKeywordsEntity commitKeyword : commitKeywords) {
            String oid = commitKeyword.getOid();
            CommitEntity commitEntity = commitRepository.findByCommitOid(oid).orElse(null);
            if (commitEntity == null) continue;

            LocalDate now = LocalDate.now();
            LocalDate commitDate = commitEntity.getCommitDate();
            if (commitDate.isAfter(oneYearAgo)) {
                long daysBetween = ChronoUnit.DAYS.between(commitDate, now);
                double recencyRatio = (365 - daysBetween) / 365.0;

                double lengthRatio = commitEntity.getLength() / maxCommitLength;

                recencyLengthScore += recencyRatio * lengthRatio;
            }
        }

        return recencyLengthScore;
    }
}

