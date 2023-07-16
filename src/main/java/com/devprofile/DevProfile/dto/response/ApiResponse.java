package com.devprofile.DevProfile.dto.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

@Getter
@Setter
@Data
public class ApiResponse<T> {

    private boolean result;
    private T data;
    private String token;
    private HttpStatus errorCode;
    private String message;


    public ApiResponse(boolean result, T data, String token, HttpStatus errorCode, String message) {
        this.result = result;
        this.data = data;
        this.token = token;
        this.errorCode = errorCode;
        this.message = message;
    }

}
