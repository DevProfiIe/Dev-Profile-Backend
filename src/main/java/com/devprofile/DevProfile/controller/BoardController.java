package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.dto.response.analyze.UserPageDTO;
import com.devprofile.DevProfile.entity.LanguageDuration;
import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.entity.UserDataEntity;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import com.devprofile.DevProfile.repository.UserDataRepository;
import com.devprofile.DevProfile.repository.UserRepository;
import com.devprofile.DevProfile.service.userData.UserDataService;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;

@Controller
public class BoardController {

    private final UserDataRepository userDataRepository;
    private final UserRepository userRepository;
    private final GitRepository gitRepository;
    private final UserDataService userDataService;

    public BoardController(UserDataRepository userDataRepository, UserRepository userRepository, GitRepository gitRepository, UserDataService userDataService) {
        this.userDataRepository = userDataRepository;
        this.userRepository = userRepository;
        this.gitRepository = gitRepository;
        this.userDataService = userDataService;
    }

    @GetMapping("/board")
    public ResponseEntity<ApiResponse> userBoardData(){
        ApiResponse<List<UserPageDTO>> apiResponse = new ApiResponse<>();
        ModelMapper modelMapper = new ModelMapper();

        List<UserPageDTO> userList = new ArrayList<>();
        List<UserEntity> UserEntityList = userRepository.findAll();
        for(UserEntity userEntity : UserEntityList){
            UserPageDTO userPageDTO = new UserPageDTO();
            UserDataEntity userDataEntity = userDataRepository.findByUserName(userEntity.getLogin());
            if(userDataEntity == null) continue;
            List<RepositoryEntity> repositoryEntities = gitRepository.findByUserId(userEntity.getId());
            Map<String, Long> langUsage = new HashMap<>();
            for(RepositoryEntity repositoryEntity :repositoryEntities){
                List<LanguageDuration> languageDurations = repositoryEntity.getLanguageDurations();
                for(LanguageDuration languageDuration : languageDurations){
                    String lang = languageDuration.getLanguage();
                    Long usage = languageDuration.getDuration().longValue();
                    Long currentValue = langUsage.getOrDefault(lang, 0L);
                    langUsage.put(lang, currentValue + usage);
                }
            }
            List<String> techStack = new ArrayList<>();
            langUsage.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue())
                    .limit(3)
                    .forEach(entry -> techStack.add(entry.getKey()));
            userPageDTO.setTechStack(techStack);
            userPageDTO.setUserName(userEntity.getName());
            userPageDTO.setField(userDataService.findMaxFieldInList(userDataEntity.getUserName()));
            userList.add(userPageDTO);
        }
        apiResponse.setMessage(null);
        apiResponse.setToken(null);
        apiResponse.setResult(true);
        apiResponse.setData(userList);
        return ResponseEntity.ok(apiResponse);
    }
}
