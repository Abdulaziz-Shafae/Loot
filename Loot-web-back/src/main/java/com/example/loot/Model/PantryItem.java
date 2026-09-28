package com.example.loot.Model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "ingredient_id"})})
public class PantryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer id;

    @Column(columnDefinition = "int not null")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer userId;

    @NotNull( message = "The ingredient ID cant be empty")
    @Column(columnDefinition = "int not null")
    private Integer ingredientId;

    @NotNull( message = "The quantity cant be empty")
    @jakarta.validation.constraints.DecimalMin("0")
    @Check(constraints = "quantity >= 0")
    @Column(columnDefinition = "double not null")
    @jakarta.validation.constraints.DecimalMax("100000000")
    private Double quantity;

    @NotNull( message = "The low stock threshold cant be empty")
    @jakarta.validation.constraints.DecimalMin("0")
    @Check(constraints = "low_stock_threshold >= 0")
    @Column(columnDefinition = "double not null")
    @jakarta.validation.constraints.DecimalMax("100000000")
    private Double lowStockThreshold;
    @Version
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long version;
}



