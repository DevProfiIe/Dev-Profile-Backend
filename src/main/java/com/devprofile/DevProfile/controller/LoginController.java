package com.devprofile.DevProfile.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
    @GetMapping("/CustomLogin")
    public String customLogin(Model model, @AuthenticationPrincipal OAuth2User oauth2User) {
        String userName = oauth2User.getAttribute("login");
        model.addAttribute("userName", userName);
        return "CustomLogin";
    }
}
