package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.component.JwtProvider;
import com.devprofile.DevProfile.dto.response.*;
import com.devprofile.DevProfile.dto.response.analyze.CommitKeywordsDTO;
import com.devprofile.DevProfile.dto.response.analyze.RepositoryEntityDTO;
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
import com.devprofile.DevProfile.service.search.SparqlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


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

            Mono<Void> mono = Mono.when(userService.userOwnedRepositories(user), orgService.orgOwnedRepositories(user));

            filterService.createAndSaveFilter(user.getName());

            return mono;
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
        List<PatchEntity> patchEntities = patchRepository.findByCommitOid("d394b3b52e18f89773ed02af5163d91d62cfd797");
        patchEntities.forEach(patchEntity -> gptPatchService.generateKeyword(userName, patchEntity));
        return "index";
    }



    @GetMapping("/response_test")
    public ResponseEntity<ApiResponse<Object>> responseApiTest(@RequestParam String userName) {
        List<CommitEntity> allCommitEntities = commitRepository.findAll();
        Map<String, CommitKeywordsDTO> oidAndKeywordsMap = new HashMap<>();
        Map<LocalDate, Integer> calender = new HashMap<>();

        for (CommitEntity commitEntity : allCommitEntities) {
            Optional<CommitKeywordsDTO> keywords = responseService.getFeatureFramework(commitEntity.getCommitOid());
            if (keywords.isPresent()) {
                oidAndKeywordsMap.put(commitEntity.getCommitOid(), keywords.get());
            }
            LocalDate day = commitEntity.getCommitDate();
            calender.merge(day, 1, Integer::sum);
        }
        List<Map<String, Object>> calenderData = new ArrayList<>();
        LocalDate firstDate = LocalDate.now();
        LocalDate lastDate = LocalDate.MIN;
        for( LocalDate day : calender.keySet() ){
            if(firstDate.isAfter(day)) firstDate = day;
            if(lastDate.isBefore(day)) lastDate = day;
            Map<String, Object> oneDay = new HashMap<>();
            oneDay.put("day", day);
            oneDay.put("value", calender.get(day));
            calenderData.add(oneDay);
        }

        repositoryService.saveFrameworksToNewTable(allCommitEntities, oidAndKeywordsMap);
        List<RepositoryEntity> repositoryEntities = gitRepository.findWithCommitAndEndDate();

        List<RepositoryEntityDTO> extendedEntities = repositoryService.createRepositoryEntityDTOs(repositoryEntities, allCommitEntities, oidAndKeywordsMap);

        UserEntity userEntity = userRepository.findByLogin(userName);
        UserDataEntity userDataEntity = userDataRepository.findByUserName(userName);
        UserDTO userDTO = null;
        if (userEntity != null) {
            userDTO = convertToDTO(userEntity, userDataEntity);
            userDTO.setCommitCalender(calenderData);
            userDTO.setCommitStart(firstDate);
            userDTO.setCommitEnd(lastDate);
        }

        String message = null;
        if (userDTO == null) {
            message = "Fail: No user found";
        } else if (extendedEntities.isEmpty()) {
            message = "Fail: No data found";
        }

        Map<String, Object> combinedData = new HashMap<>();
        combinedData.put("userInfo", userDTO);
        combinedData.put("repositoryInfo", extendedEntities);


        ApiResponse<Object> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userDTO != null && !extendedEntities.isEmpty());
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
            userBoardResponse = userBoardData();
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
            int finalScore = (int) Math.round(normalizedScoreCount + normalizedScoreRecencyLength);

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

