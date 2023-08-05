package com.devprofile.DevProfile.controller;


import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.entity.FilterEntity;
import com.devprofile.DevProfile.entity.UserStatusEntity;
import com.devprofile.DevProfile.repository.UserStatusRepository;
import com.devprofile.DevProfile.service.userData.UserStatusService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@AllArgsConstructor
public class MyPageController {

    private final UserStatusRepository userStatusRepository;
    private final UserStatusService userStatusService;

    @GetMapping("/myPage")
    public ResponseEntity<ApiResponse> myPage(@RequestParam String userName){
//        Map<String,List<UserStatusEntity>> userStatusEntities = new ArrayList<>();
        ApiResponse<Map<String, Object>> apiResponse = new ApiResponse<>();
        apiResponse.setToken(null);
        apiResponse.setMessage(null);
        apiResponse.setResult(true);
        apiResponse.setData(userStatusService.myPage(userName));

        return ResponseEntity.ok(apiResponse);
    }
}
