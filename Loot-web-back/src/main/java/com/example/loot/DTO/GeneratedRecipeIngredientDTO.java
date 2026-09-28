package com.example.loot.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@lombok.NoArgsConstructor
@Data
@AllArgsConstructor
public class GeneratedRecipeIngredientDTO {

    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Integer ingredientId;

    private String name;

    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.DecimalMin(value="0", inclusive=false)
    @jakarta.validation.constraints.DecimalMax("100000000")
    private Double quantity;

    private String unit;
}
