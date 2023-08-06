package com.devprofile.DevProfile.service;
import com.devprofile.DevProfile.entity.FrameworkEntity;
import com.devprofile.DevProfile.repository.FrameworkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
public class FrameworkService {
    @Autowired
    private FrameworkRepository frameworkRepository;
    public List<String> getAllFrameworkNames() {
        List<FrameworkEntity> frameworkEntities = frameworkRepository.findAll();
        return frameworkEntities.stream().map(FrameworkEntity::getFrameworkName).collect(Collectors.toList());
    }
    public List<Map<String, String>> getFrameworkUrls(List<String> frameworkNames) {
        List<FrameworkEntity> frameworks = frameworkRepository.findAllByFrameworkNameIn(frameworkNames);
        List<Map<String, String>> frameworksDTO = new ArrayList<>();
        for(FrameworkEntity framework :frameworks){
            Map<String, String> frameworkDTO = new HashMap<>();
            frameworkDTO.put("skill" , framework.getFrameworkName());
            frameworkDTO.put("url", framework.getFrameworkUrl());
            frameworksDTO.add(frameworkDTO);
        }
        return frameworksDTO;
    }


}