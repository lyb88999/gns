package com.gns.notification.security;

import com.gns.notification.exception.UnauthorizedException;

public final class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(UserContext context) {
        CONTEXT.set(context);
    }

    public static UserContext get() {
        return CONTEXT.get();
    }

    public static UserContext requireUser() {
        UserContext context = CONTEXT.get();
        if (context == null) {
            throw new UnauthorizedException("用户未登录或上下文缺失");
        }
        return context;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
