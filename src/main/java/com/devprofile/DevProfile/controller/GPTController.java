package com.devprofile.DevProfile.controller;


import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.service.gpt.GptPatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class GPTController {

    private final GptPatchService gptPatchService;

    @PostMapping("/analyze/keywords")
    public ResponseEntity<ApiResponse> analyzeKeywords(@RequestParam String userName) throws Exception {
        gptPatchService.generateSentence(userName);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/analyze/patches")
    public ResponseEntity<ApiResponse> testGpt(@RequestParam String userName) {
        gptPatchService.processAllEntities(userName);
        return ResponseEntity.ok(new ApiResponse());
    }
}
