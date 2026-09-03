package com.hiacademy.api.controller;
import com.hiacademy.api.entity.UserRole;
import com.hiacademy.api.security.TokenDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
public class AuthHelper {
    public static Long academyId(Authentication auth) { return details(auth).academyId(); }
    public static Long subjectId(Authentication auth) { return details(auth).subjectId(); }
    public static String role(Authentication auth) { return details(auth).role(); }
    public static boolean isDirector(Authentication auth) {
        return UserRole.ADMIN.name().equalsIgnoreCase(role(auth));
    }
    private static TokenDetails details(Authentication auth) {
        return (TokenDetails)((UsernamePasswordAuthenticationToken)auth).getDetails();
    }
}
