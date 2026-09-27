package com.example.loot.Security;

import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;
import java.util.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean HttpSessionSecurityContextRepository contextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean HttpSessionCsrfTokenRepository csrfRepository() { return new HttpSessionCsrfTokenRepository(); }
    @Bean SecurityFilterChain security(HttpSecurity http, HttpSessionSecurityContextRepository contexts,
            HttpSessionCsrfTokenRepository csrf, SessionGuard guard, CorsConfigurationSource cors) throws Exception {
        return http.cors(c -> c.configurationSource(cors)).csrf(c -> c.csrfTokenRepository(csrf))
            .securityContext(c -> c.securityContextRepository(contexts))
            .requestCache(c -> c.disable())
            .formLogin(c -> c.disable()).httpBasic(c -> c.disable()).logout(c -> c.disable())
            .authorizeHttpRequests(a -> a
                .requestMatchers("/error", "/api/v1/user/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/user/add", "/api/v1/user/login", "/api/v1/user/forgot-password", "/api/v1/user/reset-password").permitAll()
                .requestMatchers("/api/v1/admin/**", "/api/v1/user/get", "/api/v1/user/delete/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/ingredient/**", "/api/v1/system/**").authenticated()
                .requestMatchers("/api/v1/ingredient/**", "/api/v1/system/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().denyAll())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req,res,ex) -> { res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Please sign in\"}"); })
                .accessDeniedHandler((req,res,ex) -> { res.setStatus(403); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Request not permitted. Refresh and try again.\"}"); }))
            .addFilterAfter(guard, SecurityContextHolderFilter.class)
            .build();
    }
    @Bean org.springframework.boot.web.servlet.FilterRegistrationBean<SessionGuard> guardRegistration(SessionGuard guard) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(guard);
        registration.setEnabled(false); return registration;
    }
    @Bean org.springframework.security.core.userdetails.UserDetailsService noFallbackLogin() {
        return username -> { throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Use the session login API"); };
    }
    @Bean CorsConfigurationSource cors(@Value("${loot.cors.origins}") String origins) {
        var config = new CorsConfiguration();
        var allowed = Arrays.stream(origins.split(",")).map(String::trim).toList();
        if (allowed.contains("*")) throw new IllegalArgumentException("Explicit CORS origins required");
        config.setAllowedOrigins(allowed); config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
        var source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",config); return source;
    }
}
