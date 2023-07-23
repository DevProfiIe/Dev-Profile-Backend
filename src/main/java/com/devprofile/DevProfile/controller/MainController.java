package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.component.JwtProvider;
import com.devprofile.DevProfile.entity.UserEntity;
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import reactor.core.publisher.Mono;

import java.io.IOException;

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



    @GetMapping("/main")
    public Mono<Void> main(@RequestHeader String Authorization) throws IOException {


        jwtProvider.validateToken(Authorization);
        String primaryId = jwtProvider.getIdFromJWT(Authorization);
        log.info(primaryId);
        UserEntity user = userRepository.findById(Integer.parseInt(primaryId)).orElseThrow();
        System.out.println("accessToken = " + user.getGitHubToken());

        return userService.UserSaves(user);
    }

    @GetMapping("/test/gpt")
    public String testGpt(){

        return "index";
    }
}

