package com.example.loot.Model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer id;

    @NotBlank( message = "The name cant be blank")
    @NotEmpty( message = "The name cant be empty")
    @Size(max = 100, message = "The name must not exceed 100 characters")
    @Pattern(
            regexp = "^[\\p{L}\\p{M}0-9 '-]+$",
            message = "The name must contain only letters and spaces"
    )
    @Column(columnDefinition = "VARCHAR(100) not null")
    private String name;

    @NotBlank( message = "The email cant be blank")
    @NotEmpty( message = "The email cant be empty")
    @Email
    @Size(max = 150, message = "The email must not exceed 150 characters")
    @Column(columnDefinition = "VARCHAR(150) not null unique")
    private String email;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @lombok.ToString.Exclude
    @Column(nullable = false, length = 255)
    private String password;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(nullable = false, columnDefinition = "varchar(10) default 'USER'")
    private String role = "USER";

    @com.fasterxml.jackson.annotation.JsonIgnore
    @lombok.ToString.Exclude
    @Column(name = "reset_code_hash")
    private String resetTokenHash;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.time.Instant resetExpiresAt;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer authVersion = 0;

    @NotNull(message = "The phone number cant be empty")    @Pattern(
            regexp = "^05\\d{8}$",
            message = "Phone number must be a valid Saudi phone number"
    )
    @Column(columnDefinition = "varchar(10) not null")    private String phoneNumber;
}
