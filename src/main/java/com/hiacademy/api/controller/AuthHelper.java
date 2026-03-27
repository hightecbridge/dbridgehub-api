package com.hiacademy.api.controller;
import com.hiacademy.api.security.TokenDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
class AuthHelper {
    static Long academyId(Authentication auth) { return details(auth).academyId(); }
    static Long subjectId(Authentication auth) { return details(auth).subjectId(); }
    private static TokenDetails details(Authentication auth) {
        return (TokenDetails)((UsernamePasswordAuthenticationToken)auth).getDetails();
    }
}
