package com.example.loot.Controller;

import com.example.loot.Api.ApiResponse;
import com.example.loot.Model.PantryItem;
import com.example.loot.Service.PantryItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pantry")
public class PantryItemController {

    private final PantryItemService pantryItemService;


    @GetMapping("/get")
    public ResponseEntity<?> getPantryItems(){
        return ResponseEntity.status(200).body(pantryItemService.getPantryItems());
    }


    @PostMapping("/add")
    public ResponseEntity<?> addPantryItem(@RequestBody @Valid PantryItem pantryItem, Errors errors){

        if(errors.hasErrors()){
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }

        int result = pantryItemService.addPantryItem(pantryItem);

        if(result == 2){
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if(result == 3){
            return ResponseEntity.status(400).body(new ApiResponse("Ingredient not found"));
        }

        if(result == 4){
            return ResponseEntity.status(400).body(new ApiResponse("Ingredient already exists in user's pantry"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("Pantry item added"));
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<?> editPantryItem(@PathVariable Integer id, @RequestBody @Valid PantryItem pantryItem, Errors errors){

        if(errors.hasErrors()){
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }

        int result = pantryItemService.editPantryItem(id, pantryItem);

        if(result == 0){
            return ResponseEntity.status(400).body(new ApiResponse("Pantry item not found"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("Pantry item updated"));
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deletePantryItem(@PathVariable Integer id){

        int result = pantryItemService.deletePantryItem(id);

        if(result == 0){
            return ResponseEntity.status(400).body(new ApiResponse("Pantry item not found"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("Pantry item deleted"));
    }
}