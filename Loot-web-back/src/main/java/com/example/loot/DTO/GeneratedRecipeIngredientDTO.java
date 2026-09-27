package com.example.loot.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@lombok.NoArgsConstructor
@Data
@AllArgsConstructor
public class GeneratedRecipeIngredientDTO {

    private Integer ingredientId;

    private String name;

    private Double quantity;

    private String unit;
}