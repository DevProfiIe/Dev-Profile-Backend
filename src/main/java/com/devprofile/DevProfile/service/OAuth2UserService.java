package com.devprofile.DevProfile.service;

import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Integer userId = (Integer) oAuth2User.getAttributes().get("id");
        String userName = (String) oAuth2User.getAttributes().get("login");
        String userImg = (String) oAuth2User.getAttributes().get("avatar_url");
        String userEmail = (String) oAuth2User.getAttributes().get("email");
        String userNickName = (String) oAuth2User.getAttributes().get("name");

        if (userId == null || userName == null || userImg == null || userEmail == null || userNickName == null) {
            throw new IllegalArgumentException("Invalid OAuth2User attributes");
        }

        if (!userRepository.existsByUserId(userId)) {
            UserEntity newUser = new UserEntity();
            newUser.setUserId(userId);
            newUser.setUserName(userName);
            newUser.setUserEmail(userEmail);
            newUser.setUserImg(userImg);
            newUser.setUserNickName(userNickName);
            userRepository.save(newUser);
        }

        return oAuth2User;
    }

}
