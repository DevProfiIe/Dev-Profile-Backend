package com.devprofile.DevProfile.service;


import com.devprofile.DevProfile.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
public class GitLoginService {

    @Value("${spring.security.oauth2.client.registration.github.clientId}")
    private String clientId ;  // GitHub 애플리케이션의 Client ID
    @Value("${spring.security.oauth2.client.registration.github.clientSecret}")
    private String clientSecret; // GitHub 애플리케이션의 Client Secret
    @Value("${spring.security.oauth2.client.registration.github.redirect-uri}")
    private String redirectUri;  // GitHub 애플리케이션의 Redirect URI

    public String getAccessToken(String code) {
        String url = "https://github.com/login/oauth/access_token";

        // POST 요청에 필요한 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // POST 요청에 필요한 Body 설정
        Map<String, Object> body = new HashMap<>();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("code", code);
        body.put("redirect_uri", redirectUri);

        WebClient webClient = WebClient.create();

        // HTTP 요청 생성
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = webClient.post()
                .uri(url)
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .bodyValue(body)
                .retrieve()
                .toEntity(Map.class)
                .block();
        System.out.println("response = " + response);


        // 응답으로 오는 엑세스 토큰 반환
        if (response.getStatusCode() == HttpStatus.OK) {
            return  response.getBody().get("access_token").toString();
        } else {
            // 실패 시 오류 처리
            throw new RuntimeException("Failed to get access token");
        }
    }


    public UserEntity getUserInfo(String accessToken) {
        String url = "https://api.github.com/user";
        WebClient webClient = WebClient.create();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        ResponseEntity<UserEntity> response = webClient.get()
                .uri(url)
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .toEntity(UserEntity.class)
                .block();

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            // 실패 시 오류 처리
            throw new RuntimeException("Failed to get user information");
        }
    }
}
