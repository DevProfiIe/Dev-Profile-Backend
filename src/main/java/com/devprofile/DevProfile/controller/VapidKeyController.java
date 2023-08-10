package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.dto.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VapidKeyController {

    @Value("${vapid.public-key}")
    private String vapidPublicKey;

    @GetMapping("/vapidPublicKey")
    public ResponseEntity<ApiResponse<String>> getVapidPublicKey() {
        ApiResponse<String> response = new ApiResponse<>(true, vapidPublicKey, null, "Success");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
