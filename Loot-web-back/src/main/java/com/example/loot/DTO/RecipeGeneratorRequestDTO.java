package com.example.loot.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@lombok.NoArgsConstructor
@Data
@AllArgsConstructor
public class RecipeGeneratorRequestDTO {

    @NotBlank(message = "Request cannot be empty")
    @jakarta.validation.constraints.Size(max = 2000)
    private String request;
}
