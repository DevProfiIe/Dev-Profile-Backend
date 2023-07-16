package com.devprofile.DevProfile.config;

import com.devprofile.DevProfile.service.OAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;
@Configuration
@EnableWebSecurity
public class SecurityConfig {


    @Autowired
    private OAuth2UserService oAuth2UserService;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeRequests(
                        auth -> auth.anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .oauth2Login(httpSecurityOAuth2LoginConfigurer -> httpSecurityOAuth2LoginConfigurer
                        .defaultSuccessUrl("/CustomLogin")
                        .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService)))
                .csrf((csrf) -> csrf.disable());

        return http.build();
    }
}
