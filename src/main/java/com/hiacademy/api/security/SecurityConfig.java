package com.hiacademy.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);

        http.csrf(AbstractHttpConfigurer::disable)
            .cors(c -> c.configurationSource(corsConfigSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                // 브라우저 CORS preflight(OPTIONS)가 403 나지 않도록
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Actuator health → /platform/health (management.endpoints.web.base-path)
                .requestMatchers(EndpointRequest.to("health")).permitAll()
                .requestMatchers("/platform/**").permitAll()
                // 홈페이지 무료상담: MVC 매칭 + 일부 인프라에서 보이는 /api 접두 경로
                .requestMatchers(mvc.pattern(HttpMethod.POST, "/homepage/consults")).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/homepage/consults").permitAll()
                .requestMatchers(mvc.pattern(HttpMethod.POST, "/homepage/signup-inquiries")).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/homepage/signup-inquiries").permitAll()
                .requestMatchers(mvc.pattern(HttpMethod.POST, "/homepage/parent-inquiries")).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/homepage/parent-inquiries").permitAll()
                .requestMatchers(mvc.pattern(HttpMethod.POST, "/academy/admin/auth/signup")).permitAll()
                .requestMatchers(mvc.pattern(HttpMethod.POST, "/academy/admin/auth/login")).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/academy/admin/auth/signup").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/academy/admin/auth/login").permitAll()
                .requestMatchers("/academy/parent/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/academy/parent/academy/**").permitAll()
                .requestMatchers("/academy/admin/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/academy/parent/**").hasRole("PARENT")
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigSource() {
        var c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.amplifyapp.com",
                "https://*.dbridgehub.com",
                "https://dbridgehub.com",
                "https://www.dbridgehub.com",
                "https://*.hiacademy.co.kr",
                "https://hiacademy.co.kr",
                "https://www.hiacademy.co.kr",
                "exp://*"));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true);
        var s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
