package com.example.loot.Controller;

import com.example.loot.DTO.AuthRequests.*;
import com.example.loot.Model.User;
import com.example.loot.Api.ApiResponse;
import com.example.loot.Service.AccountService;
import com.example.loot.Security.CurrentUser;
import com.example.loot.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.http.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class AuthController {
    private final AccountService accounts;
    private final CurrentUser current;
    private final UserRepository users;
    private final HttpSessionSecurityContextRepository contexts;
    private final HttpSessionCsrfTokenRepository csrf;
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) { return Map.of("token",token.getToken(),"headerName",token.getHeaderName()); }
    @PostMapping("/add") public Map<String,Object> register(@RequestBody @Valid Register dto, HttpServletRequest req, HttpServletResponse res) {
        User user=accounts.register(dto);
        try {
            authenticate(user,req,res);
            return Map.of("message","Account created","authenticated",true);
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
            var session=req.getSession(false);
            if(session!=null) session.invalidate();
            return Map.of("message","Account created. You can now sign in.","authenticated",false);
        }
    }
    @PostMapping("/login") public User login(@RequestBody @Valid Login dto, HttpServletRequest req,HttpServletResponse res) {
        User user=accounts.login(dto);
        authenticate(user,req,res); return user;
    }
    private void authenticate(User user,HttpServletRequest req,HttpServletResponse res) {
        req.getSession(); req.changeSessionId();
        var context=SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user.getId().toString(),null,List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole()))));
        SecurityContextHolder.setContext(context); contexts.saveContext(context,req,res);
        req.getSession().setAttribute("userId",user.getId()); req.getSession().setAttribute("authVersion",user.getAuthVersion()); req.getSession().setAttribute("role",user.getRole());
        csrf.saveToken(null,req,res);
    }
    @PostMapping("/logout") public ApiResponse logout(HttpServletRequest req,HttpServletResponse res) {
        csrf.saveToken(null,req,res); var session=req.getSession(false); if(session!=null)session.invalidate(); SecurityContextHolder.clearContext();
        return new ApiResponse("Logged out");
    }
    @GetMapping("/me") public User me() { return current.user(); }
    @PutMapping("/me") public User profile(@RequestBody @Valid Profile dto) { var u=current.user(); u.setName(dto.name().trim());u.setPhoneNumber(dto.phoneNumber());return users.save(u); }
    @PutMapping("/me/password") public ApiResponse password(@RequestBody @Valid Password dto,HttpServletRequest req,HttpServletResponse res) { accounts.changePassword(current.user(),dto);return logout(req,res); }
    @PostMapping("/forgot-password") public ApiResponse forgot(@RequestBody @Valid Forgot dto) { accounts.forgot(dto.email());return new ApiResponse("If an account exists for this email, a password reset link has been sent. Email delivery may take some time."); }
    @PostMapping("/reset-password") public ResponseEntity<ApiResponse> reset(@RequestBody @Valid Reset dto) { boolean ok=accounts.reset(dto);return ResponseEntity.status(ok ? 200 : 400).body(new ApiResponse(ok ? "Password reset. Please sign in." : "Invalid or expired reset link")); }
    @GetMapping("/get") public List<User> users() { return users.findAll(); }
}
