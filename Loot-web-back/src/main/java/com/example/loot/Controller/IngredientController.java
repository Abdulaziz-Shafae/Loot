package com.example.loot.Controller;

import com.example.loot.Api.ApiResponse;
import com.example.loot.Model.Ingredient;
import com.example.loot.Service.IngredientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ingredient")
public class IngredientController {

    private final IngredientService ingredientService;


    @GetMapping("/get")
    public ResponseEntity<?> getIngredients(){
        return ResponseEntity.status(200).body(ingredientService.getIngredients());
    }


    @PostMapping("/add")
    public ResponseEntity<?> addIngredient(@RequestBody @Valid Ingredient ingredient, Errors errors){

        if(errors.hasErrors()){
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }

        int result = ingredientService.addIngredient(ingredient);

        if(result == 2){
            return ResponseEntity.status(400).body(new ApiResponse("Ingredient name already taken"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("Ingredient added"));
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<?> editIngredient(@PathVariable Integer id, @RequestBody @Valid Ingredient ingredient, Errors errors){

        if(errors.hasErrors()){
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }

        int result = ingredientService.editIngredient(id, ingredient);

        if(result == 0){
            return ResponseEntity.status(400).body(new ApiResponse("Ingredient not found"));
        }

        if(result == 2){
            return ResponseEntity.status(400).body(new ApiResponse("Ingredient name already taken"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("Ingredient updated"));
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteIngredient(@PathVariable Integer id){

        int result = ingredientService.deleteIngredient(id);

        if(result == 0){
            return ResponseEntity.status(400).body(new ApiResponse("Ingredient not found"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("Ingredient deleted"));
    }
}