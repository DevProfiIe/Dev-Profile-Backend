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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
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
    public ResponseEntity<ApiResponse<List<RepositoryEntityDTO>>> responseApiTest() {
        List<CommitEntity> allCommitEntities = commitRepository.findAll();
        Map<String, CommitKeywordsDTO> oidAndKeywordsMap = new HashMap<>();

        for (CommitEntity commitEntity : allCommitEntities) {
            Optional<CommitKeywordsDTO> keywords = responseService.getFeatureFramework(commitEntity.getCommitOid());
            if (keywords.isPresent()) {
                oidAndKeywordsMap.put(commitEntity.getCommitOid(), keywords.get());
            }
        }

        List<RepositoryEntity> repositoryEntities = gitRepository.findWithCommitAndEndDate();

        List<RepositoryEntityDTO> extendedEntities = repositoryService.createRepositoryEntityDTOs(repositoryEntities, allCommitEntities, oidAndKeywordsMap);

        ApiResponse<List<RepositoryEntityDTO>> apiResponse = new ApiResponse<>();
        apiResponse.setResult(!extendedEntities.isEmpty());
        apiResponse.setData(extendedEntities);
        apiResponse.setMessage(extendedEntities.isEmpty() ? "No data found" : "Data fetched successfully");

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

