package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.dto.response.UserDTO;
import com.devprofile.DevProfile.dto.response.UserPageDTO;
import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.entity.UserDataEntity;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.GitRepository;
import com.devprofile.DevProfile.repository.UserDataRepository;
import com.devprofile.DevProfile.repository.UserRepository;
import com.devprofile.DevProfile.service.userData.UserDataService;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
                Set<String> langList = repositoryEntity.getRepoLanguages();
                if(repositoryEntity.getStartDate() ==null || repositoryEntity.getEndDate() ==null) continue;
                Long usage = ChronoUnit.DAYS.between(repositoryEntity.getEndDate(), repositoryEntity.getStartDate());
                for(String lang : langList){
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
