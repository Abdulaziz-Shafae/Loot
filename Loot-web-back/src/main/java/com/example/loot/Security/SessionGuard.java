package com.example.loot.Security;

import com.example.loot.Repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import java.io.IOException;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class SessionGuard extends OncePerRequestFilter {
    private final UserRepository users;
    private final RateLimits limits;
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        var session = req.getSession(false);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && session != null && session.getAttribute("userId") instanceof Integer id) {
            var user = users.findUserById(id);
            if (user == null || !Objects.equals(user.getAuthVersion(), session.getAttribute("authVersion")) || !Objects.equals(user.getRole(),session.getAttribute("role"))) {
                session.invalidate(); SecurityContextHolder.clearContext();
                res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Please sign in again\"}"); return;
            }
        }
        String path=req.getRequestURI();
        if ("POST".equals(req.getMethod()) && (path.startsWith("/api/v1/ai/") || path.contains("password") || path.endsWith("/login") || path.endsWith("/user/add") || path.endsWith("/email"))) {
            boolean ai=path.startsWith("/api/v1/ai/");
            String key=(ai ? "ai:" : "sensitive:")+req.getRemoteAddr();
            if (!limits.allow(key, ai ? 30 : 40, 900) || (ai && session != null && !limits.allow("ai-user:"+session.getAttribute("userId"),20,900))) {
                res.setStatus(429); res.setHeader("Retry-After","900"); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Too many requests. Please try again later.\"}"); return;
            }
        }
        chain.doFilter(req,res);
    }
}
