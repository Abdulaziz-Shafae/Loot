package com.example.loot.Controller;

import com.example.loot.Api.ApiResponse;
import com.example.loot.DTO.AIRecipeDTO;
import com.example.loot.DTO.GeneratedRecipeDTO;
import com.example.loot.DTO.ImageToIngredientDTO;
import com.example.loot.DTO.IngredientSubstituteDTO;
import com.example.loot.DTO.LeftoverDTO;
import com.example.loot.DTO.RecipeGeneratorRequestDTO;
import com.example.loot.Service.AIService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;
    private final jakarta.validation.Validator validator;
    private final com.example.loot.Repository.IngredientRepository ingredients;
    private final com.example.loot.Repository.PantryItemRepository pantry;

    @PostMapping("/image/to/ingredient")
    public ResponseEntity<?> imageToIngredient(@RequestParam(value = "image", required = false) MultipartFile image, HttpSession session) {

        ImageToIngredientDTO result = aiService.imageToIngredient((Integer) session.getAttribute("userId"), image);

        // User not found
        if (result == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        // No image
        if (result.getName().equals("no img")) {
            return ResponseEntity.status(400).body(new ApiResponse("No image uploaded"));
        }

        // Wrong image type
        if (result.getName().equals("wrong img")) {
            return ResponseEntity.status(400).body(new ApiResponse("Image must be JPEG, PNG, or WEBP"));
        }

        // AI response format problem
        if (result.getName().equals("ai format error")) {
            return ResponseEntity.status(400).body(new ApiResponse("AI response format error"));
        }

        // Image processing problem
        if (result.getName().equals("image error")) {
            return ResponseEntity.status(400).body(new ApiResponse("Could not process image"));
        }

        // AI could not identify the ingredient
        if (result.getName().equals("not identified")) {
            return ResponseEntity.status(200).body(new ApiResponse("Ingredient could not be identified"));
        }

        return ResponseEntity.status(200).body(result);
    }

    @PostMapping("/image/to/ingredient/add")
    public ResponseEntity<?> addImageIngredient(@RequestBody @Valid ImageToIngredientDTO imageToIngredientDTO, HttpSession session) {

        int result = aiService.addImageIngredient((Integer) session.getAttribute("userId"), imageToIngredientDTO);


        if (result == 0) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if (result == 1) {
            return ResponseEntity.status(200).body(new ApiResponse("New ingredient added to ingredients and pantry"));
        }

        if (result == 2) {
            return ResponseEntity.status(200).body(new ApiResponse("Ingredient added to pantry"));
        }

        if (result == 3) {
            return ResponseEntity.status(200).body(new ApiResponse("Pantry ingredient quantity updated"));
        }

        if (result == 4) {
            return ResponseEntity.status(400).body(new ApiResponse("Invalid ingredient data"));
        }

        if (result == 5) {
            return ResponseEntity.status(400).body(new ApiResponse("Ingredient unit does not match"));
        }


        return ResponseEntity.status(400).body(new ApiResponse("Something went wrong"));
    }

    @PostMapping("/ingredient/substitute/{recipeId}/{listType}/{ingredientId}")
    public ResponseEntity<?> ingredientSubstitute(@PathVariable Integer recipeId, @PathVariable String listType, @PathVariable Integer ingredientId, HttpSession session) {

        IngredientSubstituteDTO result = aiService.ingredientSubstitute((Integer) session.getAttribute("userId"), recipeId, listType, ingredientId);


        if (result == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }


        if (result.getMissingIngredient().equals("ingredient not found")) {
            return ResponseEntity.status(400).body(new ApiResponse("Ingredient not found"));
        }


        if (result.getMissingIngredient().equals("recipe not found")) {
            return ResponseEntity.status(400).body(new ApiResponse("Recipe not found"));
        }


        if (result.getMissingIngredient().equals("ingredient not in recipe")) {
            return ResponseEntity.status(400).body(new ApiResponse("Ingredient is not part of this recipe"));
        }


        if (result.getMissingIngredient().equals("wrong list type")) {
            return ResponseEntity.status(400).body(new ApiResponse("List type must be system or user"));
        }


        if (result.getSubstitute().equals("no substitute")) {
            return ResponseEntity.status(200).body(new ApiResponse("You do not have a suitable substitute in your pantry."));
        }


        if (result.getSubstitute().equals("ai format error")) {
            return ResponseEntity.status(400).body(new ApiResponse("AI response format error"));
        }


        return ResponseEntity.status(200).body(result);
    }

    @PostMapping("/recipe/recommendation")
    public ResponseEntity<?> recipeRecommendation(@RequestBody @Valid RecipeGeneratorRequestDTO requestDTO, Errors errors, HttpSession session) {

        if (errors.hasErrors()) {
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }


        List<AIRecipeDTO> result = aiService.recipeRecommendation((Integer) session.getAttribute("userId"), requestDTO.getRequest());


        if (result == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }


        if (result.isEmpty()) {
            return ResponseEntity.status(400).body(new ApiResponse("AI could not recommend recipes"));
        }


        return ResponseEntity.status(200).body(result);
    }

    @PostMapping("/leftover/rescue")
    public ResponseEntity<?> leftoverRescue(@RequestBody @Valid List<LeftoverDTO> leftovers, Errors errors, HttpSession session) {

        if (errors.hasErrors()) {
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }


        if (leftovers == null || leftovers.isEmpty()) {
            return ResponseEntity.status(400).body(new ApiResponse("Leftovers cannot be empty"));
        }


        if (leftovers.size()>30 || leftovers.stream().anyMatch(x -> x==null || !validator.validate(x).isEmpty() || !Double.isFinite(x.getQuantity()) || x.getName().length()>100))
            return ResponseEntity.badRequest().body(new ApiResponse("Check leftover ingredients"));
        var seen = new java.util.HashSet<Integer>();
        Integer userId = (Integer) session.getAttribute("userId");
        for (var leftover : leftovers) {
            var ingredient = ingredients.findIngredientByNameIgnoreCase(leftover.getName().trim());
            var stock = ingredient == null ? null : pantry.findPantryItemByUserIdAndIngredientId(userId, ingredient.getId());
            if (stock == null || !seen.add(ingredient.getId()) || !ingredient.getUnit().equals(leftover.getUnit()) || leftover.getQuantity() > stock.getQuantity())
                return ResponseEntity.badRequest().body(new ApiResponse("Choose unique pantry ingredients with valid units and available quantities"));
            leftover.setName(ingredient.getName());
        }
        List<AIRecipeDTO> result = aiService.leftoverRescue(userId, leftovers);


        if (result == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }


        if (result.isEmpty()) {
            return ResponseEntity.status(400).body(new ApiResponse("AI could not rescue the leftovers"));
        }


        return ResponseEntity.status(200).body(result);
    }

    @PostMapping("/recipe/generator")
    public ResponseEntity<?> recipeGenerator(@RequestBody @Valid RecipeGeneratorRequestDTO requestDTO, Errors errors, HttpSession session) {

        if (errors.hasErrors()) {
            return ResponseEntity.status(400).body(errors.getFieldError().getDefaultMessage());
        }

        Integer userId = (Integer) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        GeneratedRecipeDTO result = aiService.recipeGenerator(userId, requestDTO.getRequest());

        if (result == null) {
            return ResponseEntity.status(400).body(new ApiResponse("Could not generate recipe"));
        }

        return ResponseEntity.status(200).body(result);
    }

    @PostMapping("/recipe/generator/add")
    public ResponseEntity<?> addGeneratedRecipe(@RequestBody @Valid GeneratedRecipeDTO recipeDTO, HttpSession session) {

        int result = aiService.addGeneratedRecipe((Integer) session.getAttribute("userId"), recipeDTO);


        if (result == 0) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }


        if (result == 1) {
            return ResponseEntity.status(200).body(new ApiResponse("Generated recipe added successfully"));
        }


        if (result == 2) {
            return ResponseEntity.status(400).body(new ApiResponse("Recipe name already exists"));
        }


        if (result == 3) {
            return ResponseEntity.status(400).body(new ApiResponse("Invalid recipe data"));
        }


        if (result == 4) {
            return ResponseEntity.status(400).body(new ApiResponse("Invalid recipe ingredient"));
        }


        return ResponseEntity.status(400).body(new ApiResponse("Something went wrong"));
    }

}
