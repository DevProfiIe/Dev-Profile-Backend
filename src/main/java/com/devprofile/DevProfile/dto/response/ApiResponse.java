package com.devprofile.DevProfile.dto.response;

import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

@Getter
@Setter
@Data
@NoArgsConstructor
public class ApiResponse<T> {
    private boolean result;
    private T data;
    private String token;
    private String message;
}
