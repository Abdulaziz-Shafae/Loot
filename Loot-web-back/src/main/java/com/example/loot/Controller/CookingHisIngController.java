package com.example.loot.Controller;

import com.example.loot.Api.ApiResponse;
import com.example.loot.Model.CookingHisIng;
import com.example.loot.Service.CookingHisIngService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/history/ingredient")
public class CookingHisIngController {

    private final CookingHisIngService cookingHisIngService;


    @GetMapping("/get")
    public ResponseEntity<?> getCookingHisIng(){
        return ResponseEntity.status(200).body(cookingHisIngService.getCookingHisIng());
    }


    @PostMapping("/add")
    public ResponseEntity<?> addCookingHisIng(@RequestBody @Valid CookingHisIng cookingHisIng, Errors errors){

        if(errors.hasErrors()){
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }

        int result = cookingHisIngService.addCookingHisIng(cookingHisIng);

        if(result == 2){
            return ResponseEntity.status(400).body(new ApiResponse("Cooking history not found"));
        }

        if(result == 3){
            return ResponseEntity.status(400).body(new ApiResponse("Ingredient not found"));
        }

        if(result == 4){
            return ResponseEntity.status(400).body(new ApiResponse("Ingredient already exists in cooking history"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("Cooking history ingredient added"));
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<?> editCookingHisIng(@PathVariable Integer id, @RequestBody @Valid CookingHisIng cookingHisIng, Errors errors){

        if(errors.hasErrors()){
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }

        int result = cookingHisIngService.editCookingHisIng(id, cookingHisIng);

        if(result == 0){
            return ResponseEntity.status(400).body(new ApiResponse("Cooking history ingredient not found"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("Cooking history ingredient updated"));
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCookingHisIng(@PathVariable Integer id){

        int result = cookingHisIngService.deleteCookingHisIng(id);

        if(result == 0){
            return ResponseEntity.status(400).body(new ApiResponse("Cooking history ingredient not found"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("Cooking history ingredient deleted"));
    }
}