package com.devprofile.DevProfile.service.userData;


import com.devprofile.DevProfile.entity.FilterEntity;
import com.devprofile.DevProfile.entity.UserStatusEntity;
import com.devprofile.DevProfile.repository.FilterRepository;
import com.devprofile.DevProfile.repository.UserScoreRepository;
import com.devprofile.DevProfile.repository.UserStatusRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class UserStatusService {
    private final UserStatusRepository userStatusRepository;
    private final FilterRepository filterRepository;

    public Map<String, Object> myPage(String userLogin) {
        Map<String, Object> myPageList = new HashMap<>();
        List<UserStatusEntity> userSendEntities = userStatusRepository.findBySendUserLogin(userLogin);
        Map<String, List<FilterEntity>> sendStatus = new HashMap<>();
        for (UserStatusEntity userStatusEntity : userSendEntities) {
            List<FilterEntity> filterEntities = sendStatus.getOrDefault(userStatusEntity.getUserStatus(), new ArrayList<>());
            filterEntities.add(filterRepository.findByUserLogin(userLogin));
            sendStatus.put(userStatusEntity.getUserStatus(), filterEntities);
        }
        myPageList.put("send", userSendEntities);
        List<UserStatusEntity> userReceiveEntities = userStatusRepository.findByReceiveUserLogin(userLogin);
        for (UserStatusEntity userStatusEntity : userReceiveEntities) {
            List<FilterEntity> filterEntities = sendStatus.getOrDefault(userStatusEntity.getUserStatus(), new ArrayList<>());
            filterEntities.add(filterRepository.findByUserLogin(userLogin));
            sendStatus.put(userStatusEntity.getUserStatus(), filterEntities);
        }
        myPageList.put("receive", userReceiveEntities);

        return myPageList;
    }
}
