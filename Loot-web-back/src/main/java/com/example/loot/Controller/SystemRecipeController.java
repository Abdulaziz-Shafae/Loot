package com.example.loot.Controller;

import com.example.loot.Api.ApiResponse;
import com.example.loot.Model.SystemRecipe;
import com.example.loot.Service.SystemRecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/system")
public class SystemRecipeController {

    private final SystemRecipeService systemRecipeService;


    @GetMapping("/get")
    public ResponseEntity<?> getSystemRecipes(){
        return ResponseEntity.status(200).body(systemRecipeService.getSystemRecipes());
    }


    @PostMapping("/add")
    public ResponseEntity<?> addSystemRecipe(@RequestBody @Valid SystemRecipe systemRecipe, Errors errors){

        if(errors.hasErrors()){
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }

        int result = systemRecipeService.addSystemRecipe(systemRecipe);

        if(result == 2){
            return ResponseEntity.status(400).body(new ApiResponse("System recipe name already taken"));
        }

        return ResponseEntity.status(200).body(java.util.Map.of("message", "Recipe added", "id", systemRecipe.getId()));
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<?> editSystemRecipe(@PathVariable Integer id, @RequestBody @Valid SystemRecipe systemRecipe, Errors errors){

        if(errors.hasErrors()){
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }

        int result = systemRecipeService.editSystemRecipe(id, systemRecipe);

        if(result == 0){
            return ResponseEntity.status(400).body(new ApiResponse("System recipe not found"));
        }

        if(result == 2){
            return ResponseEntity.status(400).body(new ApiResponse("System recipe name already taken"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("System recipe updated"));
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteSystemRecipe(@PathVariable Integer id){

        int result = systemRecipeService.deleteSystemRecipe(id);

        if(result == 0){
            return ResponseEntity.status(400).body(new ApiResponse("System recipe not found"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("System recipe deleted"));
    }
}