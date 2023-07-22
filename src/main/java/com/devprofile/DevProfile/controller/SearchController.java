package com.devprofile.DevProfile.controller;


import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.repository.CommitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SearchController {

    @Autowired
    CommitRepository commitRepository;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchCommit(@RequestParam String query){
        query = "%" + query + "%";
        List<CommitEntity> commitEntityList = commitRepository.findExistingCommitMessage(query);
        ApiResponse<List<CommitEntity>> apiResponse = new ApiResponse<>();
        apiResponse.setMessage(null);
        apiResponse.setData(commitEntityList);
        apiResponse.setToken(null);
        apiResponse.setResult(true);

        return  ResponseEntity.ok(apiResponse);
    }
}
