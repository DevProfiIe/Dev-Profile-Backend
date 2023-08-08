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
import org.apache.coyote.Response;
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
    private final ListRepository listRepository;
    private final FilterRepository filterRepository;
    private final FilterService filterService;


    private MypageDTO convertToMyPageDTO(ListEntity listEntity, String userName){
        MypageDTO mypageDTO = new MypageDTO();
        mypageDTO.setStatus(listEntity.getStatus());
        mypageDTO.setPeople(listEntity.getPeople());
        mypageDTO.setId(listEntity.getId());
        mypageDTO.setFilter(listEntity.getFilter());
        mypageDTO.setUserName(userName);

        return mypageDTO;
    }
    @GetMapping("/myPage")
    public ResponseEntity<ApiResponse> myPage(@RequestParam String userName){
        Map<String,Object> userStatusEntities = new HashMap<>();
        Map<String, List<MypageDTO>> userPageSend = new HashMap<>();
        Map<String, List<MypageDTO>> userPageReceive = new HashMap<>();
        List<ListEntity> userStatusSend=listRepository.findBySendUserLogin(userName);
        List<ListEntity> userStatusReceive=listRepository.findByReceiveUserLogin(userName);
        List<MypageDTO> userPageOngoing = new ArrayList<>();
        List<MypageDTO> userPageEnd = new ArrayList<>();

        for(ListEntity listEntity: userStatusSend){
            String uniqueName = listEntity.getReceiveUserLogin();
            if(listEntity.getStatus()) userPageEnd.add(convertToMyPageDTO(listEntity, uniqueName));
            else userPageOngoing.add(convertToMyPageDTO(listEntity, uniqueName));
        }

        userPageSend.put("onGoing",userPageOngoing);
        userPageSend.put("end", userPageEnd);
        userPageOngoing = new ArrayList<>();
        userPageEnd = new ArrayList<>();

        for(ListEntity listEntity: userStatusReceive){
            String uniqueName = listEntity.getSendUserLogin();
            if(listEntity.getStatus()) userPageEnd.add(convertToMyPageDTO(listEntity, uniqueName));
            else userPageOngoing.add(convertToMyPageDTO(listEntity, uniqueName));
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

    @GetMapping("/myPage/specific")
    public ResponseEntity<ApiResponse> myPageSpecific(@RequestParam String id){
        ListEntity listEntity = listRepository.findById(id).orElseThrow();
        List<FilterEntity> filterEntityList = new ArrayList<>();
        ApiResponse<List<FilterEntity>> apiResponse = new ApiResponse<>();
        for(String filterName : listEntity.getFilteredNameList()){
            FilterEntity filterEntity  = filterRepository.findByUserLogin(filterName);
            filterEntityList.add(filterEntity);
        }
        apiResponse.setToken(null);
        apiResponse.setMessage(null);
        apiResponse.setResult(true);
        apiResponse.setData(filterEntityList);
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
