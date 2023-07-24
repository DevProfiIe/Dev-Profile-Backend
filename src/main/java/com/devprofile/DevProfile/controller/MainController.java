package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.component.JwtProvider;
import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.UserDataRepository;
import com.devprofile.DevProfile.repository.UserRepository;
import com.devprofile.DevProfile.service.GitLoginService;
import com.devprofile.DevProfile.service.gpt.GPTService;
import com.devprofile.DevProfile.service.graphql.GraphUserService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Set;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);


    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final JwtProvider jwtProvider;

    @Autowired
    private final GitLoginService gitLoginService;

    @Autowired
    private final GraphUserService userService;

    @Autowired
    private final GPTService gptService;

    @Autowired
    private final UserDataRepository userDataRepository;



    @GetMapping("/main")
    public Mono<Void> main(@RequestHeader String Authorization) throws IOException {


        jwtProvider.validateToken(Authorization);
        String primaryId = jwtProvider.getIdFromJWT(Authorization);
        log.info(primaryId);
        UserEntity user = userRepository.findById(Integer.parseInt(primaryId)).orElseThrow();
        System.out.println("accessToken = " + user.getGitHubToken());

        return userService.UserSaves(user);
    }

    @GetMapping("/user/keyword")
    public ResponseEntity<ApiResponse> giveKeywords(@RequestParam String userName){
        ApiResponse<Set<String>> apiResponse = new ApiResponse<>();
        apiResponse.setResult(true);
        apiResponse.setData(userDataRepository.findByUserName(userName).getKeywordSet());
        apiResponse.setMessage(null);
        apiResponse.setToken(null);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/test/gpt")
    public String testGpt(@RequestParam String userName){
        gptService.processAllEntities(userName);
        return "index";
    }
}

