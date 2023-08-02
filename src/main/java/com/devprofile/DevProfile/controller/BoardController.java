package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.dto.response.analyze.UserPageDTO;
import com.devprofile.DevProfile.entity.*;
import com.devprofile.DevProfile.repository.*;
import com.devprofile.DevProfile.service.userData.UserDataService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;

@Controller
@AllArgsConstructor
public class BoardController {

    private final UserDataRepository userDataRepository;
    private final UserRepository userRepository;
    private final GitRepository gitRepository;
    private final UserDataService userDataService;
    private final RepoFrameworkRepository repoFrameworkRepository;
    private final FrameworkRepository frameworkRepository;

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
            Map<String, Long> frameUsage = new HashMap<>();
            for(RepositoryEntity repositoryEntity :repositoryEntities){
                List<LanguageDuration> languageDurations = repositoryEntity.getLanguageDurations();
                List<RepoFrameworkEntity> frameworkDurations = repoFrameworkRepository.findAllByRepoName(repositoryEntity.getRepoName());
                for(LanguageDuration languageDuration : languageDurations){
                    String lang = languageDuration.getLanguage();
                    Long usage = languageDuration.getDuration().longValue();
                    Long currentValue = langUsage.getOrDefault(lang, 0L);
                    langUsage.put(lang, currentValue + usage);
                }
                for(RepoFrameworkEntity repoFramework : frameworkDurations){
                    String framework = repoFramework.getFramework();
                    String frameworkUrl = frameworkRepository.findByFrameworkName(framework).getFrameworkUrl();
                    Long usage = repoFramework.getRepoDuration();
                    Long currentValue = frameUsage.getOrDefault(frameworkUrl, 0L);
                    frameUsage.put(frameworkUrl, currentValue+usage);
                }
            }
            List<String> language = new ArrayList<>();
            List<String> framework = new ArrayList<>();
            langUsage.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .forEach(entry -> language.add(entry.getKey()));
            frameUsage.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .forEach(entry -> framework.add(entry.getKey()));
            userPageDTO.setLanguage(language);
            userPageDTO.setFramework(framework);
            userPageDTO.setUserName(userEntity.getName());
            userPageDTO.setField(userDataService.findMaxFieldInList(userDataEntity.getUserName()));
            userPageDTO.setAvatarUrl(userEntity.getAvatar_url());
            userList.add(userPageDTO);
        }
        apiResponse.setMessage(null);
        apiResponse.setToken(null);
        apiResponse.setResult(true);
        apiResponse.setData(userList);
        return ResponseEntity.ok(apiResponse);
    }

}
