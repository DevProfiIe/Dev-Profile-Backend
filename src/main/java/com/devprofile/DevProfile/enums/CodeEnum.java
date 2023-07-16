package com.devprofile.DevProfile.enums;

public enum CodeEnum {


    // 성공 200번대
    SUCCESS("200", "success"),
    CREATED("201", "created"),

    // BAD_REQUEST 1~ 200 번대
    EXPIRED_TOKEN("001", "expired jwt token"),
    INVALID_TOKEN_FORMAT("002", "invalid jwt token format"),
    NULL_OR_BLANK_TOKEN("003", "null or blank jwt token"),
    EMPTY_VALUE("004", "null or empty value exception"),

    // UNAUTHORIZED 300번대
    INVALID_PROVIDER_TYPE("300","invalid provider type"),
    OAUTH_PROVIDER_MISS_MATCH("301","oauth provider miss match"),
    AUTHENTICATION_EXCEPTION("302","oauth authentication exception"),
    USERNAME_NOT_FOUND("303", "username not found"),

    // NOT_FOUND 400번대
    NOT_FOUND_REFRESH_TOKEN("400", "not found refresh token"),
    NOT_FOUND_USER("401", "not found user"),
    NOT_FOUND_CHANNEL_USER("410", "not found channel user"),
    NOT_FOUND_CHANNEL("411", "not found channel"),
    NOT_FOUND_REVIEW("412", "not found review"),

    // UNKNOWN
    UNKNOWN_EXCEPTION("500", "unknownException"),

    // CUSTOM 500번대
    // 530 채널
    CHANNEL_GET_FAILED("530", "channel get failed"),
    CHANNEL_POST_FAILED("531", "channel post failed"),
    CHANNEL_PATCH_FAILED("532", "channel patch failed"),
    CHANNEL_DELETE_FAILED("533", "channel delete failed"),
    CHANNEL_USER_GET_FAILED("534", "channel users get failed"),
    CHANNEL_USER_POST_FAILED("535", "channel users post failed"),
    CHANNEL_USER_DELETE_FAILED("536", "channel user delete failed"),
    CHANNEL_USER_ALREADY_EXIST("537", "user already exists in the channel."),

    // 540 컨텐츠
    NO_CONTENT_TYPE("540", "contentType required"),

    // 550 리뷰
    REVIEW_GET_FAILED("550", "review get failed"),
    REVIEW_POST_FAILED("551", "review post failed"),

    //560 알림
    USER_NOTICE_GET_FAILED("560", "user notice get failed"),
    POST_USER_NOTICE_FAILED("561", "user notice post failed");

    private String code;
    private String message;

    CodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
