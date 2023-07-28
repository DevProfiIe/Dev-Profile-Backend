package com.devprofile.DevProfile.controller;


import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.devprofile.DevProfile.service.patch.PatchService;
import com.devprofile.DevProfile.service.search.SearchService;
import com.devprofile.DevProfile.similaritySearch.Embedding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin
@Controller
public class SearchController {

    @Autowired
    CommitRepository commitRepository;

    @Autowired
    Embedding embedding;

    @Autowired
    SearchService searchService;


    @Autowired
    private final PatchService patchService;

    public SearchController(PatchService patchService) {
        this.patchService = patchService;
    }

    @GetMapping("/startModel")
    public ResponseEntity<?> startModel() {
        embedding.loadModel();
        return ResponseEntity.ok(null);
    }


    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchCommit(@RequestParam String query) {
        query = "%" + query + "%";
        List<CommitEntity> commitEntityList = commitRepository.findExistingCommitMessage(query);
        ApiResponse<List<CommitEntity>> apiResponse = new ApiResponse<>();
        apiResponse.setMessage(null);
        apiResponse.setData(commitEntityList);
        apiResponse.setToken(null);
        apiResponse.setResult(true);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/search/similarity")
    public ResponseEntity<ApiResponse> searchSimilarCommit(@RequestParam String query) {
        ApiResponse<List<CommitEntity>> apiResponse = new ApiResponse<>();

        List<CommitEntity> searchResultList =new ArrayList<>();
        for(String oid : searchService.getTop10LevenshteinSimilarEntity(query)){

            searchResultList.add(commitRepository.findByCommitOid(oid).orElseThrow());
        }
        apiResponse.setMessage(null);
        apiResponse.setData(searchResultList);
        apiResponse.setToken(null);
        apiResponse.setResult(true);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/search/commits")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> searchDetailCommits(@RequestParam String commitOid, @RequestHeader String Authorization) {
        ApiResponse<Map<String, Object>> apiResponse = new ApiResponse<>();
        Map<String, Object> combinedData = new HashMap<>();
        List<Map<String, Object>> diffList = new ArrayList<>();

        return patchService.getPatchesByCommitOid(commitOid)
                .concatMap(patch -> {
                    String contentsUrl = patch.getContentsUrl();

                    return patchService.fetchCode(contentsUrl, Authorization)
                            .map(decodedCode -> {
                                Map<String, Object> diff = patchService.analyzeDiff(patch.getPatch(), decodedCode);
                                diffList.add(diff);
                                return diff;
                            });
                })
                .then(Mono.just(diffList))
                .map(diffListData -> {
                    combinedData.put("diffs", diffListData);
                    apiResponse.setResult(true);
                    apiResponse.setData(combinedData);
                    return ResponseEntity.ok(apiResponse);
                });
    }
}
