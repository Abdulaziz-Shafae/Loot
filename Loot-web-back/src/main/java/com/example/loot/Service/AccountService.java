package com.example.loot.Service;

import com.example.loot.DTO.AuthRequests.*;
import com.example.loot.Model.User;
import com.example.loot.Repository.*;
import com.example.loot.Security.RateLimits;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.security.SecureRandom;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccountService {
    // Temporarily disabled: email delivery can exceed reset-code validity.
    @org.springframework.beans.factory.annotation.Value("${features.email-password-reset.enabled:false}")
    private boolean emailPasswordResetEnabled;
    private void requireEmailPasswordReset() {
        if (!emailPasswordResetEnabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Email password reset is temporarily unavailable");
    }
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final EmailService email;
    private final RateLimits limits;
    private final SecureRandom random = new SecureRandom();
    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    public void strong(String password) {
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72 || !password.matches("(?s)(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,72}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use 8–72 characters with uppercase, lowercase, a number and a symbol");
    }
    public User register(Register dto) {
        strong(dto.password());
        String address=normalize(dto.email());
        if (users.findUserByEmail(address)!=null) throw new ResponseStatusException(HttpStatus.CONFLICT,"Unable to create account with these details");
        var u=new User(); u.setName(dto.name().trim()); u.setEmail(address); u.setPhoneNumber(dto.phoneNumber());
        u.setPassword(encoder.encode(dto.password())); u.setRole("USER"); users.save(u);
        // Registration remains usable if the optional welcome message cannot be delivered.
        try { email.sendWelcomeEmail(u.getEmail(),u.getName()); } catch (com.example.loot.Service.EmailDeliveryException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Welcome email could not be delivered"); }
        return u;
    }
    public User login(Login dto) {
        String address=normalize(dto.email());
        if (!limits.allow("login:"+address,10,900)) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many attempts. Try again later.");
        User u=users.findUserByEmail(address);
        // No plaintext fallback: legacy accounts must reset their password.
        String hash=u==null ? "$2a$12$GEX9NHdlWECojCJqHDLCyu6ONnmMNz4BhTOAy3lMcHZaCNH00QWWS" : u.getPassword();
        boolean valid=encoder.matches(dto.password(),hash);
        if (u==null || !valid) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email or password is incorrect");
        return u;
    }
    @Transactional
    public void forgot(String value) {
        requireEmailPasswordReset();
        String address=normalize(value);
        if (!limits.allow("reset-mail:"+address,3,900)) return;
        User u=users.lockByEmail(address);
        if (u==null) return;
        int code=random.nextInt(900000)+100000;
        u.setResetCodeHash(encoder.encode(String.valueOf(code))); u.setResetExpiresAt(Instant.now().plusSeconds(600)); u.setResetAttempts(0);
        try { email.sendVerificationCode(address,u.getName(),code); }
        catch(com.example.loot.Service.EmailDeliveryException e) { u.setResetCodeHash(null); u.setResetExpiresAt(null); org.slf4j.LoggerFactory.getLogger(getClass()).warn("Reset email could not be delivered"); }
        users.save(u);
    }
    @Transactional
    public boolean reset(Reset dto) {
        requireEmailPasswordReset();
        strong(dto.password());
        String address=normalize(dto.email());
        if (!limits.allow("reset-verify:"+address,10,900)) return false;
        User u=users.lockByEmail(address);
        if (u==null || u.getResetCodeHash()==null || u.getResetExpiresAt()==null || u.getResetExpiresAt().isBefore(Instant.now()) || u.getResetAttempts()>=5) return false;
        u.setResetAttempts(u.getResetAttempts()+1);
        if (!encoder.matches(dto.code(),u.getResetCodeHash())) { users.save(u); return false; }
        u.setPassword(encoder.encode(dto.password())); clearReset(u); u.setAuthVersion(u.getAuthVersion()+1); users.save(u); return true;
    }
    public void changePassword(User u, Password dto) {
        strong(dto.password());
        if (!encoder.matches(dto.currentPassword(),u.getPassword())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Current password is incorrect");
        u.setPassword(encoder.encode(dto.password())); clearReset(u); u.setAuthVersion(u.getAuthVersion()+1); users.save(u);
    }
    private void clearReset(User u) { u.setResetCodeHash(null); u.setResetExpiresAt(null); u.setResetAttempts(0); }
}
