package com.devprofile.DevProfile.service;

import com.devprofile.DevProfile.entity.FrameworkEntity;
import com.devprofile.DevProfile.repository.FrameworkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public Map<String, String> getFrameworkUrls(List<String> frameworkNames) {
        List<FrameworkEntity> frameworks = frameworkRepository.findAllByFrameworkNameIn(frameworkNames);
        return frameworks.stream().collect(Collectors.toMap(FrameworkEntity::getFrameworkName, FrameworkEntity::getFrameworkUrl));
    }
}
