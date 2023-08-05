package com.devprofile.DevProfile.controller;


import com.devprofile.DevProfile.MockupDataGenerater;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class MockupController {

    private final MockupDataGenerater mockupDataGenerater;
    @PostMapping("/mockup")
    public ResponseEntity<String> mockUpData(@RequestParam Integer num){
        mockupDataGenerater.generateMockFilterEntities(num);
        return ResponseEntity.ok(num.toString());
    }
}
