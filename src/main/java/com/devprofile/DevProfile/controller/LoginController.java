package com.devprofile.DevProfile.controller;


import com.devprofile.DevProfile.component.JwtProvider;
import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.UserRepository;
import com.devprofile.DevProfile.service.GitLoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;



@RestController
@Slf4j
public class LoginController {

    @Autowired
    private GitLoginService gitLoginService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @PostMapping("/oauth2/token")
    public ResponseEntity<ApiResponse> login(@RequestParam String code) {
        System.out.println("checking post");
        log.info("check login Start");
        String accessToken= gitLoginService.getAccessToken(code);
        ApiResponse<UserEntity> apiResponse = new ApiResponse<UserEntity>();
        System.out.println(accessToken);

        if (accessToken == null || accessToken.isEmpty()) {
            apiResponse.setToken(null);
            apiResponse.setData(null);
            apiResponse.setResult(false);
            apiResponse.setMessage("Access Token request failed.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiResponse);
        }

        UserEntity user = gitLoginService.getUserInfo(accessToken);
        //수정
        String jwtToken = jwtProvider.createJwt(user);

        user.setJwtToken(jwtToken);
        user.setGitHubToken(accessToken);

        if(!userRepository.existsById(user.getId())){
            userRepository.save(user);
        }

        user.setJwtToken(null);
        user.setGitHubToken(null);

        apiResponse.setToken(jwtToken);
        apiResponse.setData(user);
        apiResponse.setResult(true);
        apiResponse.setMessage(null);
        // 정상적으로 Access Token을 받아왔다면, 이를 반환합니다.
        return ResponseEntity.ok(apiResponse);
    }
}



