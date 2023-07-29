package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.dto.response.UserDTO;
import com.devprofile.DevProfile.entity.UserDataEntity;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.UserDataRepository;
import com.devprofile.DevProfile.repository.UserRepository;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;


@Controller
public class BoardController {

    private final UserDataRepository userDataRepository;
    private final UserRepository userRepository;

    public BoardController(UserDataRepository userDataRepository, UserRepository userRepository) {
        this.userDataRepository = userDataRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/board")
    public ResponseEntity<ApiResponse> userBoardData(){
        ApiResponse<List<UserDTO>> apiResponse = new ApiResponse<>();
        ModelMapper modelMapper = new ModelMapper();

        List<UserDTO> userList = new ArrayList<>();
        List<UserEntity> UserEntityList = userRepository.findAll();
        for(UserEntity userEntity : UserEntityList){
            UserDataEntity userDataEntity = userDataRepository.findByUserName(userEntity.getLogin());
            UserDTO userDTO = new UserDTO();
            if(userDataEntity != null){
                userDTO = modelMapper.map(userDataEntity,UserDTO.class);
            }
            userDTO.setAvatar_url(userEntity.getAvatar_url());
            userDTO.setLogin(userEntity.getLogin());
            userDTO.setName(userEntity.getName());
            userList.add(userDTO);
        }
        apiResponse.setMessage(null);
        apiResponse.setToken(null);
        apiResponse.setResult(true);
        apiResponse.setData(userList);
        return ResponseEntity.ok(apiResponse);
    }
}
