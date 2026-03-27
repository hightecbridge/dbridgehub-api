package com.hiacademy.api.security;
import com.hiacademy.api.entity.Parent;
import com.hiacademy.api.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${app.jwt.secret}")           private String secret;
    @Value("${app.jwt.expiration-ms}")    private long   expirationMs;
    @Value("${app.jwt.parent-expiration-ms}") private long parentExpirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public String generateAdminToken(User user) {
        return Jwts.builder().subject(user.getId().toString())
            .claim("role", user.getRole().name()).claim("type","admin")
            .claim("academyId", user.getAcademy()!=null?user.getAcademy().getId():null)
            .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+expirationMs))
            .signWith(key()).compact();
    }
    public String generateParentToken(Parent parent) {
        return Jwts.builder().subject(parent.getId().toString())
            .claim("role","PARENT").claim("type","parent")
            .claim("academyId", parent.getAcademy().getId())
            .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+parentExpirationMs))
            .signWith(key()).compact();
    }
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }
    public boolean isValid(String token) {
        try { parse(token); return true; } catch (Exception e) { return false; }
    }
    public Long   getSubjectId(String token) { return Long.parseLong(parse(token).getSubject()); }
    public String getRole(String token)       { return parse(token).get("role",String.class); }
    public String getType(String token)       { return parse(token).get("type",String.class); }
    public Long   getAcademyId(String token)  { return parse(token).get("academyId",Long.class); }
}
