package com.hiacademy.api.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 원장 이메일 `admin@hiacademy.co.kr` + 아이디 `judy` → `judy.admin.hiacademy.co.kr` */
public final class TeacherLoginIds {
    private TeacherLoginIds() {}

    public static String suffixFromDirectorEmail(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase().replace("@", ".");
    }

    public static String normalizeHandle(String handle) {
        return handle == null ? "" : handle.trim().toLowerCase();
    }

    public static void validateHandle(String handle) {
        String h = normalizeHandle(handle);
        if (!h.matches("^[a-z0-9][a-z0-9_-]{0,30}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "교사 아이디는 영문 소문자·숫자로 시작하고, 영문/숫자/하이픈/밑줄만 사용할 수 있습니다.");
        }
    }

    public static String loginId(String handle, String directorEmail) {
        validateHandle(handle);
        return normalizeHandle(handle) + "." + suffixFromDirectorEmail(directorEmail);
    }

    public static String handleFromLoginId(String loginId, String directorEmail) {
        if (loginId == null) return "";
        String suffix = suffixFromDirectorEmail(directorEmail);
        String prefix = "." + suffix;
        if (!suffix.isBlank() && loginId.endsWith(prefix) && loginId.length() > prefix.length()) {
            return loginId.substring(0, loginId.length() - prefix.length());
        }
        return loginId;
    }
}
