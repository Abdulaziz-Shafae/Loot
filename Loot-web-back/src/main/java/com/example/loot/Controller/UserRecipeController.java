package com.example.loot.Controller;

import com.example.loot.Api.ApiResponse;
import com.example.loot.Model.UserRecipe;
import com.example.loot.Service.UserRecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recipe")
public class UserRecipeController {

    private final UserRecipeService userRecipeService;


    @GetMapping("/get")
    public ResponseEntity<?> getUserRecipes(){
        return ResponseEntity.status(200).body(userRecipeService.getUserRecipes());
    }


    @PostMapping("/add")
    public ResponseEntity<?> addUserRecipe(@RequestBody @Valid UserRecipe userRecipe, Errors errors){

        if(errors.hasErrors()){
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }

        int result = userRecipeService.addUserRecipe(userRecipe);

        if(result == 2){
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if(result == 3){
            return ResponseEntity.status(400).body(new ApiResponse("Recipe name already exists for this user"));
        }

        return ResponseEntity.status(200).body(java.util.Map.of("message", "Recipe added", "id", userRecipe.getId()));
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<?> editUserRecipe(@PathVariable Integer id, @RequestBody @Valid UserRecipe userRecipe, Errors errors){

        if(errors.hasErrors()){
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }

        int result = userRecipeService.editUserRecipe(id, userRecipe);

        if(result == 0){
            return ResponseEntity.status(400).body(new ApiResponse("User recipe not found"));
        }

        if(result == 3){
            return ResponseEntity.status(400).body(new ApiResponse("Recipe name already exists for this user"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("User recipe updated"));
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteUserRecipe(@PathVariable Integer id){

        int result = userRecipeService.deleteUserRecipe(id);

        if(result == 0){
            return ResponseEntity.status(400).body(new ApiResponse("User recipe not found"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("User recipe deleted"));
    }
}