package com.zs.ytbx.common.auth;

public final class SessionConstants {

    public static final String SESSION_USER = "ytbx:session:user";
    public static final String SESSION_USER_ID = "ytbx:user:id";
    public static final String SESSION_USERNAME = "ytbx:user:username";
    public static final String SESSION_USER_TYPE = "ytbx:user:type";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String AUTH_COOKIE_NAME = "ytbx-auth-token";

    private SessionConstants() {
    }
}
