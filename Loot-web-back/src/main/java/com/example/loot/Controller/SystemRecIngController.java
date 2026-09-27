package com.example.loot.Controller;

import com.example.loot.Api.ApiResponse;
import com.example.loot.Model.SystemRecIng;
import com.example.loot.Service.SystemRecIngService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/system/ingredient")
public class SystemRecIngController {

    private final SystemRecIngService systemRecIngService;


    @GetMapping("/get")
    public ResponseEntity<?> getSystemRecIngs(){
        return ResponseEntity.status(200).body(systemRecIngService.getSystemRecIngs());
    }


    @PostMapping("/add")
    public ResponseEntity<?> addSystemRecIng(@RequestBody @Valid SystemRecIng systemRecIng, Errors errors){

        if(errors.hasErrors()){
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }

        int result = systemRecIngService.addSystemRecIng(systemRecIng);

        if(result == 2){
            return ResponseEntity.status(400).body(new ApiResponse("System recipe not found"));
        }

        if(result == 3){
            return ResponseEntity.status(400).body(new ApiResponse("Ingredient not found"));
        }

        if(result == 4){
            return ResponseEntity.status(400).body(new ApiResponse("Ingredient already exists in system recipe"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("System recipe ingredient added"));
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<?> editSystemRecIng(@PathVariable Integer id, @RequestBody @Valid SystemRecIng systemRecIng, Errors errors){

        if(errors.hasErrors()){
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }

        int result = systemRecIngService.editSystemRecIng(id, systemRecIng);

        if(result == 0){
            return ResponseEntity.status(400).body(new ApiResponse("System recipe ingredient not found"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("System recipe ingredient updated"));
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteSystemRecIng(@PathVariable Integer id){

        int result = systemRecIngService.deleteSystemRecIng(id);

        if(result == 0){
            return ResponseEntity.status(400).body(new ApiResponse("System recipe ingredient not found"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("System recipe ingredient deleted"));
    }
}