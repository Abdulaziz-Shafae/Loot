package com.example.loot.Controller;

import com.example.loot.Api.ApiResponse;
import com.example.loot.Model.CookingHistory;
import com.example.loot.Service.CookingHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/history")
public class CookingHistoryController {

    private final CookingHistoryService cookingHistoryService;


    @GetMapping("/get")
    public ResponseEntity<?> getCookingHistory(){
        return ResponseEntity.status(200).body(cookingHistoryService.getCookingHistory());
    }


    @PostMapping("/add")
    public ResponseEntity<?> addCookingHistory(@RequestBody @Valid CookingHistory cookingHistory, Errors errors){

        if(errors.hasErrors()){
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }

        int result = cookingHistoryService.addCookingHistory(cookingHistory);

        if(result == 2){
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("Cooking history added"));
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<?> editCookingHistory(@PathVariable Integer id, @RequestBody @Valid CookingHistory cookingHistory, Errors errors){

        if(errors.hasErrors()){
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }

        int result = cookingHistoryService.editCookingHistory(id, cookingHistory);

        if(result == 0){
            return ResponseEntity.status(400).body(new ApiResponse("Cooking history not found"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("Cooking history updated"));
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCookingHistory(@PathVariable Integer id){

        int result = cookingHistoryService.deleteCookingHistory(id);

        if(result == 0){
            return ResponseEntity.status(400).body(new ApiResponse("Cooking history not found"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("Cooking history deleted"));
    }
}