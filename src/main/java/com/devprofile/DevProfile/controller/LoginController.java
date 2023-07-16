package com.devprofile.DevProfile.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;



@Controller
@RequiredArgsConstructor
public class LoginController {

    @GetMapping("/")
    public String login(Model model, @AuthenticationPrincipal OAuth2User oauth2User) {
        model.addAttribute("userName", oauth2User.getAttribute("name"));
        return "index";
    }

    @GetMapping("/CustomLogin")
    public String customLogin(Model model, @AuthenticationPrincipal OAuth2User oauth2User) {
        model.addAttribute("userName", oauth2User.getAttribute("name"));

        return "CustomLogin";
    }



}



