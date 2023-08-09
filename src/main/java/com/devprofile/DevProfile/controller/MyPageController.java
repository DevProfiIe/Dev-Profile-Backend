package com.devprofile.DevProfile.controller;


import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.dto.response.analyze.MypageDTO;
import com.devprofile.DevProfile.dto.response.analyze.UserPageDTO;
import com.devprofile.DevProfile.entity.FilterEntity;
import com.devprofile.DevProfile.entity.ListEntity;
import com.devprofile.DevProfile.entity.UserStatusEntity;
import com.devprofile.DevProfile.repository.FilterRepository;
import com.devprofile.DevProfile.repository.ListRepository;
import com.devprofile.DevProfile.repository.UserStatusRepository;
import com.devprofile.DevProfile.service.FilterService;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@AllArgsConstructor
public class MyPageController {

    private final ListRepository listRepository;
    private final FilterRepository filterRepository;
    private final FilterService filterService;


    private MypageDTO convertToMyPageDTO(ListEntity listEntity, String userName){
        MypageDTO mypageDTO = new MypageDTO();
        mypageDTO.setState(listEntity.getState());
        mypageDTO.setPeople(listEntity.getPeople());
        mypageDTO.setId(listEntity.getId());
        mypageDTO.setFilter(listEntity.getFilter());
        mypageDTO.setUserName(userName);
        mypageDTO.setSendDate(listEntity.getSendDate());

        return mypageDTO;
    }
    @GetMapping("/myPage")
    public ResponseEntity<ApiResponse> myPage(@RequestParam String userName){
        Map<String,List<MypageDTO>> userStatusEntities = new HashMap<>();

        List<ListEntity> userStatusSend = listRepository.findBySendUserLogin(userName);
        List<ListEntity> userStatusReceive = listRepository.findByReceiveUserLogin(userName);
        List<MypageDTO> userPageSend= new ArrayList<>();
        List<MypageDTO> userPageReceive = new ArrayList<>();

        for(ListEntity listEntity: userStatusSend){
            String uniqueName = listEntity.getReceiveUserLogin();
            userPageSend.add(convertToMyPageDTO(listEntity, uniqueName));
        }

        userStatusEntities.put("send",userPageSend);


        for(ListEntity listEntity: userStatusReceive){
            String uniqueName = listEntity.getSendUserLogin();
            userPageReceive.add(convertToMyPageDTO(listEntity, uniqueName));
        }
        userStatusEntities.put("receive",userPageReceive);

        ApiResponse<Map<String, List<MypageDTO>>> apiResponse = new ApiResponse<>();
        apiResponse.setToken(null);
        apiResponse.setMessage(null);
        apiResponse.setResult(true);
        apiResponse.setData(userStatusEntities);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/myPage/specific")
    public ResponseEntity<ApiResponse> myPageSpecific(@RequestParam String id){
        ListEntity listEntity = listRepository.findById(id).orElseThrow();
        List<UserPageDTO> userPageList = new ArrayList<>();
        ApiResponse<List<UserPageDTO>> apiResponse = new ApiResponse<>();
        for(String filterName : listEntity.getFilteredNameList()){
            FilterEntity filterEntity  = filterRepository.findByUserLogin(filterName);
            userPageList.add(filterService.filterChangeToDTO(filterEntity));
        }
        apiResponse.setToken(null);
        apiResponse.setMessage(null);
        apiResponse.setResult(true);
        apiResponse.setData(userPageList);
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/myPage/submit")
    public ResponseEntity<ApiResponse> myPageSubmitOngoing(@RequestBody Map<String, Object> requestBody){
        ListEntity listEntity = listRepository.findById(requestBody.get("id").toString()).orElseThrow();
        listEntity.setFilteredNameList((List<String>) requestBody.get("checkUserNames"));
        listEntity.setState("end");
        listRepository.save(listEntity);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(true);
        apiResponse.setData(null);
        apiResponse.setToken(null);
        apiResponse.setMessage(null);
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/myPage/submit/end")
    public ResponseEntity<ApiResponse> myPageSubmitEnd(@RequestBody String id){
        listRepository.deleteById(id);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(true);
        apiResponse.setData(null);
        apiResponse.setToken(null);
        apiResponse.setMessage(null);
        return ResponseEntity.ok(apiResponse);
    }
}
