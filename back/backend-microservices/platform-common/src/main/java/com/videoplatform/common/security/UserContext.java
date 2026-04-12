package com.videoplatform.common.security;

public final class UserContext {

    private static final ThreadLocal<JwtUser> USER_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(JwtUser user) {
        USER_HOLDER.set(user);
    }

    public static JwtUser get() {
        return USER_HOLDER.get();
    }

    public static Long getUserId() {
        JwtUser user = USER_HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    public static void clear() {
        USER_HOLDER.remove();
    }
}
