package com.devprofile.DevProfile.service;

import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String userName = (String) oAuth2User.getAttributes().get("login");
        String userImg = (String) oAuth2User.getAttributes().get("avatar_url");
        String userEmail = (String) oAuth2User.getAttributes().get("email");

        Optional<UserEntity> optionalUserEntity = userRepository.findByUserEmail(userEmail);
        UserEntity userEntity;
        if (!optionalUserEntity.isPresent()) {
            userEntity = new UserEntity();
            userEntity.setUserName(userName);
            userEntity.setUserEmail(userEmail);
            userEntity.setUserImg(userImg);
            userRepository.save(userEntity);
        } else {
            userEntity = optionalUserEntity.get();
        }

        return oAuth2User;
    }

}
