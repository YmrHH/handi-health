package com.example.zhinengsuifang.util;

import jakarta.servlet.http.HttpServletRequest;

public class AuthHeaderUtil {

    private AuthHeaderUtil() {
    }

    public static Long getUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String auth = request.getHeader("Authorization");
        Long id = parseLongOrNull(auth);
        if (id != null) {
            return id;
        }

        String uidHeader = request.getHeader("X-User-Id");
        id = parseLongOrNull(uidHeader);
        if (id != null) {
            return id;
        }

        String uidParam = request.getParameter("userId");
        return parseLongOrNull(uidParam);
    }

    private static Long parseLongOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        if (v.isEmpty()) {
            return null;
        }
        if (v.regionMatches(true, 0, "Bearer ", 0, 7)) {
            v = v.substring(7).trim();
        }
        if (v.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
