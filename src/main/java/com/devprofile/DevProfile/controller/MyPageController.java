package com.devprofile.DevProfile.controller;


import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.dto.response.analyze.UserPageDTO;
import com.devprofile.DevProfile.entity.FilterEntity;
import com.devprofile.DevProfile.entity.UserStatusEntity;
import com.devprofile.DevProfile.repository.FilterRepository;
import com.devprofile.DevProfile.repository.UserStatusRepository;
import com.devprofile.DevProfile.service.FilterService;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@AllArgsConstructor
public class MyPageController {

    private final UserStatusRepository userStatusRepository;
    private final FilterRepository filterRepository;
    private final FilterService filterService;

    @GetMapping("/myPage")
    public ResponseEntity<ApiResponse> myPage(@RequestParam String userName){
        Map<String,Object> userStatusEntities = new HashMap<>();
        Map<String, List<UserPageDTO>> userPageSend = new HashMap<>();
        Map<String, List<UserPageDTO>> userPageReceive = new HashMap<>();
        List<UserStatusEntity> userStatusSend=userStatusRepository.findBySendUserLogin(userName);
        List<UserStatusEntity> userStatusReceive=userStatusRepository.findByReceiveUserLogin(userName);
        List<UserPageDTO> userPageOngoing = new ArrayList<>();
        List<UserPageDTO> userPageEnd = new ArrayList<>();
        for(UserStatusEntity userStatusEntity: userStatusSend){
            FilterEntity filterEntity = filterRepository.findByUserLogin(userStatusEntity.getBoardUserLogin());
            if(filterEntity == null) continue;
            UserPageDTO userPage=filterService.filterChangeToDTO(filterEntity);
            if(userStatusEntity.getUserStatus().equals("onGoing")) userPageOngoing.add(userPage);
            else userPageEnd.add(userPage);
        }
        userPageSend.put("onGoing",userPageOngoing);
        userPageSend.put("end", userPageEnd);
        userPageOngoing = new ArrayList<>();
        userPageEnd = new ArrayList<>();

        for(UserStatusEntity userStatusEntity: userStatusReceive){
            FilterEntity filterEntity = filterRepository.findByUserLogin(userStatusEntity.getBoardUserLogin());
            if(filterEntity == null) continue;
            UserPageDTO userPage=filterService.filterChangeToDTO(filterEntity);
            if(userStatusEntity.getUserStatus().equals("onGoing")){
                userPage.setSelected(userStatusEntity.getSelectedStatus());
                userPageOngoing.add(userPage);
            }
            else userPageEnd.add(userPage);
        }
        userPageReceive.put("onGoing",userPageOngoing);
        userPageReceive.put("end", userPageEnd);

        userStatusEntities.put("send", userPageSend);
        userStatusEntities.put("receive",userPageReceive);

        ApiResponse<Map<String, Object>> apiResponse = new ApiResponse<>();
        apiResponse.setToken(null);
        apiResponse.setMessage(null);
        apiResponse.setResult(true);
        apiResponse.setData(userStatusEntities);

        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/myPage/submit")
    public ResponseEntity<ApiResponse> myPageSubmitOngoing(@RequestParam String userName){
        List<UserStatusEntity> userStatusEntities = userStatusRepository.findByReceiveUserLogin(userName);
        for(UserStatusEntity userStatusEntity: userStatusEntities){
            if(userStatusEntity.getUserStatus().equals("ongoing")){
                if(userStatusEntity.getSelectedStatus()) {
                    userStatusEntity.setUserStatus("end");
                    userStatusRepository.save(userStatusEntity);
                }
                else userStatusRepository.delete(userStatusEntity);
            }
        }
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(true);
        apiResponse.setData(null);
        apiResponse.setToken(null);
        apiResponse.setMessage(null);
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/myPage/submit/end")
    public ResponseEntity<ApiResponse> myPageSubmitEnd(@RequestParam String userName){
        List<UserStatusEntity> userStatusEntities = userStatusRepository.findBySendUserLogin(userName);
        for(UserStatusEntity userStatusEntity: userStatusEntities){
            if(userStatusEntity.getUserStatus().equals("end")){
                userStatusRepository.delete(userStatusEntity);
            }
        }
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(true);
        apiResponse.setData(null);
        apiResponse.setToken(null);
        apiResponse.setMessage(null);
        return ResponseEntity.ok(apiResponse);
    }
}
