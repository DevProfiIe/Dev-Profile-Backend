package com.devprofile.DevProfile.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PushNotificationRequest {

    private String title;
    private String body;
    private String userName;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}