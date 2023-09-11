package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.component.AggregationFilter;
import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.dto.response.analyze.UserPageDTO;
import com.devprofile.DevProfile.entity.FilterEntity;
import com.devprofile.DevProfile.entity.FrameworkEntity;
import com.devprofile.DevProfile.entity.ListEntity;
import com.devprofile.DevProfile.entity.StyleEntity;
import com.devprofile.DevProfile.repository.FrameworkRepository;
import com.devprofile.DevProfile.repository.ListRepository;
import com.devprofile.DevProfile.repository.StyleRepository;
import com.devprofile.DevProfile.repository.UserRepository;
import com.devprofile.DevProfile.request.PushNotificationRequest;
import com.devprofile.DevProfile.service.FilterService;
import com.devprofile.DevProfile.service.notification.PushNotificationService;
import com.devprofile.DevProfile.service.search.SearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@Data
class MsgRequest{
    String sendUserLogin;
    String receiveUserLogin;
    List<String> boardUserLogin;
    List<String> filterKeyword;
    List<String> filterSkill;
}

@Controller
@AllArgsConstructor
@Slf4j
public class BoardController {


    private final SearchService searchService;
    private final FrameworkRepository frameworkRepository;
    private final FilterService filterService;
    private final AggregationFilter aggregationFilter;
    private final ListRepository listRepository;
    private final StyleRepository styleRepository;
    private final PushNotificationService pushNotificationService;
    private final UserRepository userRepository;


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

        List<StyleEntity> styleList = styleRepository.findAll();
        filters.put("keyword", styleList);

        apiResponse.setData(filters);
        apiResponse.setMessage(null);
        apiResponse.setResult(true);
        apiResponse.setToken(null);

        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/board/send")
    public ResponseEntity<ApiResponse> userSendMsg(@RequestBody MsgRequest msgRequest){

        ApiResponse<String> apiResponse = new ApiResponse<>();
        ListEntity listEntity = new ListEntity();

        listEntity.setSendUserLogin(msgRequest.sendUserLogin);
        listEntity.setReceiveUserLogin(msgRequest.receiveUserLogin);
        listEntity.setPeople(msgRequest.boardUserLogin.size());
        listEntity.setState("onGoing");
        List<String> filterList = new ArrayList<>();
        for(String keywordNum : msgRequest.filterKeyword){
            filterList.add(styleRepository.findById(Integer.parseInt(keywordNum)).orElseThrow().getKeyword());
        }
        filterList.addAll(msgRequest.filterSkill);
        listEntity.setFilter(filterList);
        listEntity.setFilteredNameList(msgRequest.boardUserLogin);
        listEntity.setSendDate(LocalDate.now().toString());

        listRepository.save(listEntity);

        apiResponse.setData(null);
        apiResponse.setToken(null);
        apiResponse.setMessage(null);
        apiResponse.setResult(true);

        String recipientToken = userRepository.findTokenByUsername(msgRequest.receiveUserLogin);

        if (recipientToken == null) {
            return new ResponseEntity("Recipient token not found", HttpStatus.NOT_FOUND);
        }
        try {
            PushNotificationRequest pushRequest = new PushNotificationRequest();
            pushRequest.setTitle("DevProfile");
            pushRequest.setBody(msgRequest.sendUserLogin + "님이 " +
                    " " + listEntity.getPeople() + "명의 분석 데이터를 보냈습니다.");
            pushRequest.setUserName(msgRequest.receiveUserLogin);
            pushRequest.setToken(recipientToken);
            pushNotificationService.sendPushMessage(pushRequest);
        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }


        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/board")
    public ResponseEntity<ApiResponse> userBoardData(
            @RequestParam int page,
            @RequestParam(required = false) List<String> lang,
            @RequestParam(required = false) List<String> frame,
            @RequestParam(required = false) List<Integer> keywordsFilter,
            @RequestParam(required = false) String field) {
        ApiResponse<Map<String,Object>> apiResponse = new ApiResponse<>();
        List<UserPageDTO> resultEntities;
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


        if(keywordsFilter != null && !keywordsFilter.isEmpty()){
            for(Integer num :keywordsFilter){
                keywords.add(styles.get(num));
            }
        }
        Pair<Integer, AggregationResults<Map>> results = aggregationFilter.runAggregation(field,frame,lang,keywords, page,20 );

        resultEntities = results.getSecond().getMappedResults().stream()
                .filter(Objects::nonNull)
                .map(map -> {
                    try {
                        FilterEntity entity = objectMapper.convertValue(map, FilterEntity.class);
                        return filterService.filterChangeToDTO(entity);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


        Integer total = results.getFirst();

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total", total);
        resultMap.put("content", resultEntities);

        apiResponse.setMessage(null);
        apiResponse.setToken(null);
        apiResponse.setResult(true);
        apiResponse.setData(resultMap);
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
