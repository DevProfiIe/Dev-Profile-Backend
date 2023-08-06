package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.component.AggregationFilter;
import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.dto.response.analyze.UserPageDTO;
import com.devprofile.DevProfile.entity.*;
import com.devprofile.DevProfile.repository.*;
import com.devprofile.DevProfile.service.FilterService;
import com.devprofile.DevProfile.service.search.SearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@AllArgsConstructor
public class BoardController {


    private final FilterRepository filterRepository;
    private final SearchService searchService;
    private final FrameworkRepository frameworkRepository;
    private final FilterService filterService;
    private final AggregationFilter aggregationFilter;
    private final UserStatusRepository userStatusRepository;


    public Map<String, Object> makeKeywordMap(Integer num, String keyword){
        Map<String, Object> keywords = new HashMap<>();
        keywords.put("num", num);
        keywords.put("keyword", keyword);
        return keywords;
    }
    @GetMapping("/board/filter")
    public ResponseEntity<ApiResponse> userBoardFilter(){
        Map<String, Object> filters = new HashMap<>();
        ApiResponse<Map<String, Object>> apiResponse = new ApiResponse<>();
        List<Map<String, String>> board = new ArrayList<>();
        List<FrameworkEntity> frameworkEntities = frameworkRepository.findAll();
        for(FrameworkEntity framework: frameworkEntities){
            Map<String, String> frameworkMap = new HashMap<>();
            frameworkMap.put("sort", "framework");
            frameworkMap.put("name", framework.getFrameworkName());
            board.add(frameworkMap);
        }
        List<String> languageList = new ArrayList<>();
        languageList.add("Java");
        languageList.add("C");
        languageList.add("Ruby");
        languageList.add("C++");
        languageList.add("Python");
        languageList.add("Go");
        languageList.add("C#");
        languageList.add("JavaScript");
        languageList.add("TypeScript");
        languageList.add("PHP");
        languageList.add("Kotlin");
        languageList.add("Rust");
        languageList.add("R");
        languageList.add("Swift");
        languageList.add("Perl");
        for(String lang: languageList){
            Map<String, String> langMap = new HashMap<>();
            langMap.put("sort", "lang");
            langMap.put("name", lang);
            board.add(langMap);
        }
        filters.put("skills", board);
        List<Map<String,Object>> styleList = new ArrayList<>();

        styleList.add(makeKeywordMap(1,"지속적인 개발자"));
        styleList.add(makeKeywordMap(2,"싱글 플레이어"));
        styleList.add(makeKeywordMap(3,"멀티 플레이어"));
        styleList.add(makeKeywordMap(4, "1일 1커밋"));
        styleList.add(makeKeywordMap(5,"\"리드미\" 리드미"));
        styleList.add(makeKeywordMap(6,"인기 개발자"));
        styleList.add(makeKeywordMap(7,"영향력 있는 개발자"));
        styleList.add(makeKeywordMap(8,"새벽형 개발자"));
        styleList.add(makeKeywordMap(9,"아침형 개발자"));
        styleList.add(makeKeywordMap(10,"설명충"));
        styleList.add(makeKeywordMap(11,"주말 커밋 전문가"));
        styleList.add(makeKeywordMap(12,"주말 커밋자"));
        styleList.add(makeKeywordMap(13,"실속없는 커밋자"));
        filters.put("keyword", styleList);


        apiResponse.setData(filters);
        apiResponse.setMessage(null);
        apiResponse.setResult(true);
        apiResponse.setToken(null);

        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/board/send")
    public ResponseEntity<ApiResponse> userSendMsg(@RequestParam String sendUserName,
                                                   @RequestParam String receiveUserName,
                                                   @RequestParam List<String> boardUserNames){

        ApiResponse<String> apiResponse = new ApiResponse<>();
        List<UserStatusEntity> userStatusEntities = new ArrayList<>();
        for(String boardUserName : boardUserNames){
            UserStatusEntity userStatus = new UserStatusEntity();
            userStatus.setBoardUserLogin(boardUserName);
            userStatus.setSendUserLogin(sendUserName);
            userStatus.setReceiveUserLogin(receiveUserName);
            userStatus.setSelectedStatus(false);
            userStatus.setUserStatus("onGoing");
            userStatusEntities.add(userStatus);
        }
        userStatusRepository.saveAll(userStatusEntities);
        apiResponse.setData(null);
        apiResponse.setToken(null);
        apiResponse.setMessage(null);
        apiResponse.setResult(true);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/board")
    public ResponseEntity<ApiResponse> userBoardData(
            @RequestParam int page,
            @RequestParam(required = false) List<String> lang,
            @RequestParam(required = false) List<String> frame,
            @RequestParam(required = false) List<Integer> keywordsFilter,
            @RequestParam(required = false) String field) {
        ApiResponse<List<UserPageDTO>> apiResponse = new ApiResponse<>();
        List<FilterEntity> filterEntities = filterRepository.findAll();
        List<UserPageDTO> resultEntities = new ArrayList<>();
        List<String> keywords = new ArrayList<>();
        Map<Integer,String> styles = new HashMap<>();
        ObjectMapper objectMapper= new ObjectMapper();

        styles.put(1,"지속적인 개발자");
        styles.put(2,"싱글 플레이어");
        styles.put(3,"멀티 플레이어");
        styles.put(4, "1일 1커밋");
        styles.put(5,"\"리드미\" 리드미");
        styles.put(6,"인기 개발자");
        styles.put(7,"영향력 있는 개발자");
        styles.put(8,"새벽형 개발자");
        styles.put(9,"아침형 개발자");
        styles.put(10,"설명충");
        styles.put(11,"주말 커밋 전문가");
        styles.put(12,"주말 커밋자");
        styles.put(13,"실속없는 커밋자");

        System.out.println("field = " + field);

        if(keywordsFilter != null && !keywordsFilter.isEmpty()){
            for(Integer num :keywordsFilter){
                keywords.add(styles.get(num));
            }
        }
        AggregationResults<Map> results=aggregationFilter.runAggregation(field,frame,lang,keywords, page,20 );
        resultEntities = results.getMappedResults().stream()
                .map(map->filterService.filterChangeToDTO(objectMapper.convertValue(map,FilterEntity.class)))
                .collect(Collectors.toList());
        apiResponse.setMessage(null);
        apiResponse.setToken(null);
        apiResponse.setResult(true);
        apiResponse.setData(resultEntities);
        return ResponseEntity.ok(apiResponse);
    }



    @GetMapping("/autoKeyword")
    public ResponseEntity<ApiResponse<List<String>>> makeKeyword(@RequestParam String query){
        List<String> keywords = searchService.getCloseWords(query);
        ApiResponse<List<String>> apiResponse = new ApiResponse<>();
        apiResponse.setMessage(null);
        apiResponse.setToken(null);
        apiResponse.setResult(true);
        apiResponse.setData(keywords);
        return ResponseEntity.ok(apiResponse);
    }
}
