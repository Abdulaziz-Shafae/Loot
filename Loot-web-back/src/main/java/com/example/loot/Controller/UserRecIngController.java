package com.example.loot.Controller;

import com.example.loot.Api.ApiResponse;
import com.example.loot.Model.UserRecIng;
import com.example.loot.Service.UserRecIngService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recipe/ingredient")
public class UserRecIngController {

    private final UserRecIngService userRecIngService;


    @GetMapping("/get")
    public ResponseEntity<?> getUserRecIng(){
        return ResponseEntity.status(200).body(userRecIngService.getUserRecIng());
    }


    @PostMapping("/add")
    public ResponseEntity<?> addUserRecIng(@RequestBody @Valid UserRecIng userRecIng, Errors errors){

        if(errors.hasErrors()){
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }

        int result = userRecIngService.addUserRecIng(userRecIng);

        if(result == 2){
            return ResponseEntity.status(400).body(new ApiResponse("User recipe not found"));
        }

        if(result == 3){
            return ResponseEntity.status(400).body(new ApiResponse("Ingredient not found"));
        }

        if(result == 4){
            return ResponseEntity.status(400).body(new ApiResponse("Ingredient already exists in user recipe"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("User recipe ingredient added"));
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<?> editUserRecIng(@PathVariable Integer id, @RequestBody @Valid UserRecIng userRecIng, Errors errors){

        if(errors.hasErrors()){
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }

        int result = userRecIngService.editUserRecIng(id, userRecIng);

        if(result == 0){
            return ResponseEntity.status(400).body(new ApiResponse("User recipe ingredient not found"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("User recipe ingredient updated"));
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteUserRecIng(@PathVariable Integer id){

        int result = userRecIngService.deleteUserRecIng(id);

        if(result == 0){
            return ResponseEntity.status(400).body(new ApiResponse("User recipe ingredient not found"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("User recipe ingredient deleted"));
    }
}