package com.example.loot.DTO;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;

@lombok.NoArgsConstructor
@Data
@AllArgsConstructor
public class ImageToIngredientDTO {

    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max=100)
    @Pattern(regexp="^[\\p{L}\\p{M}0-9 '-]+$")
    private String name;

    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.DecimalMin(value="0", inclusive=false)
    @jakarta.validation.constraints.DecimalMax("100000000")
    private Double quantity;

    @Pattern(
            regexp = "^(g|ml|piece)$",
            message = "The unit must be g, ml, or piece"
    )
    @jakarta.validation.constraints.NotBlank
    private String unit;
}
