package com.example.loot.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@lombok.NoArgsConstructor
@Data
@AllArgsConstructor
public class GeneratedRecipeDTO {

    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max=150)
    @jakarta.validation.constraints.Pattern(regexp="^[\\p{L}\\p{M}0-9 '-]+$")
    private String name;

    @jakarta.validation.constraints.Size(max=500)
    private String description;

    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Pattern(regexp="^(Breakfast|Lunch|Dinner|Snack)$")
    private String category;

    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max=20000)
    private String instructions;

    @jakarta.validation.constraints.NotEmpty
    @jakarta.validation.constraints.Size(max=100)
    @jakarta.validation.Valid
    private List<@jakarta.validation.constraints.NotNull GeneratedRecipeIngredientDTO> ingredients;
}
