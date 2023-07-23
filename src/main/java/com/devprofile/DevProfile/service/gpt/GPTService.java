package com.devprofile.DevProfile.service.gpt;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GPTService {


    @Value("${gpt.url}")
    private String url;

    @Value("${gpt.secret}")
    private String key;


    public String generateKeyword(String patch){
        WebClient webClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION,"Bearer " + key)
                .build();

        String prompt = "Make one Computer Engineering or Science KeyWord about this Patch\n"
                + patch;

        // Create message objects
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        return webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of( "model", "gpt-3.5-turbo","messages" , messages))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

}
