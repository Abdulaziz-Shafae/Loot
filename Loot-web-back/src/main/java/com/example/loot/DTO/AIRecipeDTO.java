package com.example.loot.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@lombok.NoArgsConstructor
@Data
@AllArgsConstructor
public class AIRecipeDTO {

    private Integer recipeId;

    private String name;

    private String recipeType;

    private String description;

    private String category;

    private String instruction;

    private String reason;
}