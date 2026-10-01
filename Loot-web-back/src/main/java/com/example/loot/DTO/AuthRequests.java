package com.example.loot.DTO;

import jakarta.validation.constraints.*;

public final class AuthRequests {
    public record Register(@NotBlank @Size(max=100) String name, @NotBlank @Email @Size(max=150) String email,
        @NotBlank @Size(min=8,max=72) String password, @NotBlank @Pattern(regexp="^05\\d{8}$") String phoneNumber) {}
    public record Login(@NotBlank @Email @Size(max=150) String email, @NotBlank @Size(max=72) String password) {}
    public record Forgot(@NotBlank @Email @Size(max=150) String email) {}
    public record Reset(@NotBlank @Pattern(regexp="^[A-Za-z0-9_-]{43}$") String token,
        @NotBlank @Size(min=8,max=72) String newPassword) {
        @Override public String toString() { return "Reset[redacted]"; }
    }
    public record Profile(@NotBlank @Size(max=100) String name, @NotBlank @Pattern(regexp="^05\\d{8}$") String phoneNumber) {}
    public record Password(@NotBlank @Size(max=72) String currentPassword, @NotBlank @Size(min=8,max=72) String password) {}
}
