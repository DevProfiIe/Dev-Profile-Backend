package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.component.JwtProvider;
import com.devprofile.DevProfile.dto.response.*;
import com.devprofile.DevProfile.entity.*;
import com.devprofile.DevProfile.repository.*;
import com.devprofile.DevProfile.service.gpt.GPTService;
import com.devprofile.DevProfile.service.graphql.GraphOrgService;
import com.devprofile.DevProfile.service.graphql.GraphUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.Local;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@CrossOrigin
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
    private final GPTService gptService;
    private final RepositoryService repositoryService;




    private UserDTO convertToDTO(UserEntity userEntity, UserDataEntity userDataEntity) {
        UserDTO userDTO = new UserDTO();
        userDTO.setAvatar_url(userEntity.getAvatar_url());
        userDTO.setLogin(userEntity.getLogin());
        userDTO.setName(userEntity.getName());

        if (userDataEntity != null) {
            userDTO.setKeywordSet(userDataEntity.getKeywordSet());
            userDTO.setAi(userDataEntity.getAi());
            userDTO.setDataScience(userDataEntity.getDataScience());
            userDTO.setDatabase(userDataEntity.getDatabase());
            userDTO.setMobile(userDataEntity.getMobile());
            userDTO.setWebBackend(userDataEntity.getWebBackend());
            userDTO.setDocument(userDataEntity.getDocument());
            userDTO.setSystemProgramming(userDataEntity.getSystemProgramming());
            userDTO.setAlgorithm(userDataEntity.getAlgorithm());
            userDTO.setGame(userDataEntity.getGame());
        }

        return userDTO;
    }
    @GetMapping("/main")
    public Mono<Void> main(@RequestHeader String Authorization) throws IOException {
        jwtProvider.validateToken(Authorization);
        String primaryId = jwtProvider.getIdFromJWT(Authorization);
        log.info(primaryId);
        UserEntity user = userRepository.findById(Integer.parseInt(primaryId)).orElseThrow();
        System.out.println("accessToken = " + user.getGitHubToken());

        return Mono.when(userService.userOwnedRepositories(user), orgService.orgOwnedRepositories(user));

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
        gptService.processAllEntities(userName);
        return "index";
    }

    @PostMapping("/test")
    public String test(@RequestParam String userName) {
        List<PatchEntity> patchEntities = patchRepository.findByCommitOid("6e380e3a22b01d67038cecb6ffd943d6305ec346");
        patchEntities.forEach(patchEntity -> gptService.generateKeyword(userName, patchEntity));
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
            LocalDate date = commitEntity.getCommitDate();
            Integer length = commitEntity.getLength();
            calender.merge(date, length, Integer::sum);
        }
        List<Map<String, Object>> calenderData = new ArrayList<>();
        LocalDate firstDate = LocalDate.now();
        for( LocalDate date : calender.keySet() ){
            if(firstDate.isAfter(date)) firstDate = date;
            Map<String, Object> oneDay = new HashMap<>();
            oneDay.put("date", date);
            oneDay.put("value", calender.get(date));
            calenderData.add(oneDay);
        }

        List<RepositoryEntity> repositoryEntities = gitRepository.findWithCommitAndEndDate();
        List<RepositoryEntityDTO> extendedEntities = repositoryService.createRepositoryEntityDTOs(repositoryEntities, allCommitEntities, oidAndKeywordsMap);

        UserEntity userEntity = userRepository.findByLogin(userName);
        UserDataEntity userDataEntity = userDataRepository.findByUserName(userName);
        UserDTO userDTO = null;
        if (userEntity != null) {
            userDTO = convertToDTO(userEntity, userDataEntity);
            userDTO.setCommitCalender(calenderData);
            userDTO.setCommitStart(firstDate);
        }

        String userMessage = userDTO == null ? "No user found" : "User fetched successfully";

        String repositoryMessage = extendedEntities.isEmpty() ? "No data found" : "Data fetched successfully";

        Map<String, Object> combinedData = new HashMap<>();
        combinedData.put("userInfo", userDTO);
        combinedData.put("repositoryInfo", extendedEntities);

        ApiResponse<Object> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userDTO != null && !extendedEntities.isEmpty());
        apiResponse.setData(combinedData);
        apiResponse.setMessage(userMessage + "; " + repositoryMessage);

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
}

