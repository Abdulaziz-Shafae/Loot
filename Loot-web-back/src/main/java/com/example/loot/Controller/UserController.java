package com.example.loot.Controller;

import com.example.loot.Api.ApiResponse;
import com.example.loot.DTO.*;
import com.example.loot.Model.CookingHistory;
import com.example.loot.Model.User;
import com.example.loot.Service.EmailService;
import com.example.loot.Service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    @GetMapping("/can/{listType}/{recipeId}")
    public ResponseEntity<?> canCookRecipe(@PathVariable String listType, @PathVariable Integer recipeId, HttpSession session) {
        int result = userService.canCookRecipe((Integer) session.getAttribute("userId"), recipeId, listType);

        if (result == 0) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if (result == 6) {
            return ResponseEntity.status(400).body(new ApiResponse("recipe not found"));
        }

        if (result == 3) {
            return ResponseEntity.status(400).body(new ApiResponse("Insufficient ingredient quantity"));
        }

        if (result == 4) {
            return ResponseEntity.status(400).body(new ApiResponse("Missing ingredient"));
        }

        if (result == 5) {
            return ResponseEntity.status(400).body(new ApiResponse("List type must be user or system"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("You can cook this recipe"));
    }

    @GetMapping("/missing/{listType}/{recipeId}")
    public ResponseEntity<?> missingList(@PathVariable String listType, @PathVariable Integer recipeId, HttpSession session) {


        List<MissingIngredientDTO> list = userService.missingList((Integer) session.getAttribute("userId"), recipeId, listType);

        if (list == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User or recipe not found, or invalid list type"));
        }

        if (list.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("No missing ingredients, you can cook this recipe"));
        }

        return ResponseEntity.status(200).body(list);
    }

    @GetMapping("/system")
    public ResponseEntity<?> getSystemRecipes() {

        List<RecipesDTO> list = userService.getSystemRecipes();

        if (list.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("No system recipes found"));
        }

        return ResponseEntity.status(200).body(list);
    }

    @GetMapping("/system/{category}")
    public ResponseEntity<?> getSystemRecipesByCategory(@PathVariable String category) {

        if (!category.equalsIgnoreCase("breakfast") && !category.equalsIgnoreCase("lunch") && !category.equalsIgnoreCase("dinner") && !category.equalsIgnoreCase("snack")) {
            return ResponseEntity.status(400).body(new ApiResponse("Available categories: breakfast, lunch, dinner, snack"));
        }

        List<RecipesDTO> list = userService.getSystemRecipesByCategory(category);

        if (list.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("No system recipes found in this category"));
        }

        return ResponseEntity.status(200).body(list);
    }

    @GetMapping("/possible/system/{category}")
    public ResponseEntity<?> systemRecipesByCategory(@PathVariable String category, HttpSession session) {

        if (!category.equalsIgnoreCase("breakfast")
                && !category.equalsIgnoreCase("lunch")
                && !category.equalsIgnoreCase("dinner")
                && !category.equalsIgnoreCase("snack")) {

            return ResponseEntity.status(400).body(new ApiResponse("Available categories: breakfast, lunch, dinner, snack"));
        }

        List<RecipesDTO> list = userService.systemRecipesByCategory((Integer) session.getAttribute("userId"), category);

        if (list == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if (list.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("No possible recipes found in this category"));
        }

        return ResponseEntity.status(200).body(list);
    }

    @GetMapping("/almost/system/{category}")
    public ResponseEntity<?> systemRecipesByCategoryAlmost(@PathVariable String category, HttpSession session) {

        if (!category.equalsIgnoreCase("breakfast")
                && !category.equalsIgnoreCase("lunch")
                && !category.equalsIgnoreCase("dinner")
                && !category.equalsIgnoreCase("snack")) {

            return ResponseEntity.status(400).body(new ApiResponse("Available categories: breakfast, lunch, dinner, snack"));
        }

        List<AlmostRecipeDTO> list = userService.systemRecipesByCategoryAlmost((Integer) session.getAttribute("userId"), category);

        if (list == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if (list.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("No almost possible recipes found in this category"));
        }

        return ResponseEntity.status(200).body(list);
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserRecipes(HttpSession session) {

        List<RecipesDTO> list = userService.getUserRecipes((Integer) session.getAttribute("userId"));

        if (list == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if (list.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("No user recipes found"));
        }

        return ResponseEntity.status(200).body(list);
    }

    @GetMapping("/user/{category}")
    public ResponseEntity<?> getUserRecipesByCategory(@PathVariable String category, HttpSession session) {

        if (!category.equalsIgnoreCase("breakfast") && !category.equalsIgnoreCase("lunch") && !category.equalsIgnoreCase("dinner") && !category.equalsIgnoreCase("snack")) {
            return ResponseEntity.status(400).body(new ApiResponse("Available categories: breakfast, lunch, dinner, snack"));
        }

        List<RecipesDTO> list = userService.getUserRecipesByCategory((Integer) session.getAttribute("userId"), category);

        if (list == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if (list.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("No user recipes found in this category"));
        }

        return ResponseEntity.status(200).body(list);
    }

    @GetMapping("/possible/user/{category}")
    public ResponseEntity<?> userRecipesByCategory(@PathVariable String category, HttpSession session) {

        if (!category.equalsIgnoreCase("breakfast")
                && !category.equalsIgnoreCase("lunch")
                && !category.equalsIgnoreCase("dinner")
                && !category.equalsIgnoreCase("snack")) {

            return ResponseEntity.status(400).body(new ApiResponse("Available categories: breakfast, lunch, dinner, snack"));
        }

        List<RecipesDTO> list = userService.userRecipesByCategory((Integer) session.getAttribute("userId"), category);

        if (list == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if (list.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("No possible user recipes found in this category"));
        }

        return ResponseEntity.status(200).body(list);
    }

    @GetMapping("/almost/user/{category}")
    public ResponseEntity<?> userRecipesByCategoryAlmost(@PathVariable String category, HttpSession session) {

        if (!category.equalsIgnoreCase("breakfast")
                && !category.equalsIgnoreCase("lunch")
                && !category.equalsIgnoreCase("dinner")
                && !category.equalsIgnoreCase("snack")) {

            return ResponseEntity.status(400).body(new ApiResponse("Available categories: breakfast, lunch, dinner, snack"));
        }

        List<AlmostRecipeDTO> list = userService.userRecipesByCategoryAlmost((Integer) session.getAttribute("userId"), category);

        if (list == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if (list.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("No almost possible user recipes found in this category"));
        }

        return ResponseEntity.status(200).body(list);
    }

    @GetMapping("/low")
    public ResponseEntity<?> lowStock(HttpSession session) {

        List<LowStockDTO> list = userService.lowStock((Integer) session.getAttribute("userId"));

        if (list == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if (list.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("No low stock ingredients"));
        }

        return ResponseEntity.status(200).body(list);
    }

    @PostMapping("/low/email")
    public ResponseEntity<?> lowStockEmail(HttpSession session) {

        int result = userService.lowStockMSG((Integer) session.getAttribute("userId"));

        if (result == 0) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if (result == 2) {
            return ResponseEntity.status(200).body(new ApiResponse("No low stock ingredients"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("Low stock list sent to your email successfully"));
    }

    @GetMapping("/cook/{listType}/{recipeId}")
    public ResponseEntity<?> cookRecipe(@PathVariable String listType, @PathVariable Integer recipeId, HttpSession session) {

        CookingDTO  recipe = userService.getRecipeForCooking((Integer) session.getAttribute("userId"), recipeId, listType);

        if (recipe == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User or recipe not found, or invalid list type"));
        }

        return ResponseEntity.status(200).body(recipe);
    }

    @PostMapping("/cook/{listType}/{recipeId}/done")
    public ResponseEntity<?> cookRecipeDone(@PathVariable String listType, @PathVariable Integer recipeId, HttpSession session) {

        int result = userService.cookRecipe((Integer) session.getAttribute("userId"), recipeId, listType);

        if (result == 0) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if (result == 3) {
            return ResponseEntity.status(400).body(new ApiResponse("Insufficient ingredient quantity"));
        }

        if (result == 4) {
            return ResponseEntity.status(400).body(new ApiResponse("Missing ingredient"));
        }

        if (result == 5) {
            return ResponseEntity.status(400).body(new ApiResponse("List type must be user or system"));
        }

        if (result == 6) {
            return ResponseEntity.status(400).body(new ApiResponse("Recipe not found"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("Recipe cooked successfully"));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistoryByUserId(HttpSession session) {

        List<RecipesDTO> list = userService.getHistoryByUserId((Integer) session.getAttribute("userId"));

        //user not found
        if(list == null){
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if (list.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("No cooking history found"));
        }

        return ResponseEntity.status(200).body(list);
    }

    @GetMapping("/history/{category}")
    public ResponseEntity<?> getHistoryByCategory(@PathVariable String category, HttpSession session) {

        if (!category.equalsIgnoreCase("breakfast") && !category.equalsIgnoreCase("lunch") && !category.equalsIgnoreCase("dinner") && !category.equalsIgnoreCase("snack")) {
            return ResponseEntity.status(400).body(new ApiResponse("Available categories: breakfast, lunch, dinner, snack"));
        }

        List<RecipesDTO> list = userService.getHistoryByCategory((Integer) session.getAttribute("userId"), category);

        if (list == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if (list.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("No cooking history found in this category"));
        }

        return ResponseEntity.status(200).body(list);
    }

    @GetMapping("/possible/history/{category}")
    public ResponseEntity<?> possibleHistoryByCategory(@PathVariable String category, HttpSession session) {

        if (!category.equalsIgnoreCase("breakfast") && !category.equalsIgnoreCase("lunch") && !category.equalsIgnoreCase("dinner") && !category.equalsIgnoreCase("snack")) {

            return ResponseEntity.status(400).body(new ApiResponse("Available categories: breakfast, lunch, dinner, snack"));
        }

        List<RecipesDTO> list = userService.possibleHistoryByCategory((Integer) session.getAttribute("userId"), category);

        if (list == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if (list.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("No possible history recipes found in this category"));
        }

        return ResponseEntity.status(200).body(list);
    }

    @GetMapping("/almost/history/{category}")
    public ResponseEntity<?> almostHistoryByCategory(@PathVariable String category, HttpSession session) {

        if (!category.equalsIgnoreCase("breakfast") && !category.equalsIgnoreCase("lunch") && !category.equalsIgnoreCase("dinner") && !category.equalsIgnoreCase("snack")) {
            return ResponseEntity.status(400).body(new ApiResponse("Available categories: breakfast, lunch, dinner, snack"));
        }

        List<AlmostRecipeDTO> list = userService.almostHistoryByCategory((Integer) session.getAttribute("userId"), category);

        if (list == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if (list.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("No almost possible history recipes found in this category"));
        }

        return ResponseEntity.status(200).body(list);
    }

    @GetMapping("/history/{historyId}/repeat")
    public ResponseEntity<?> getPreviousCook(@PathVariable Integer historyId, HttpSession session) {

        CookingDTO  recipe = userService.getPreviousCook((Integer) session.getAttribute("userId"), historyId);

        if (recipe == null) {
            return ResponseEntity.status(400).body(new ApiResponse("User or cooking history not found"));
        }

        return ResponseEntity.status(200).body(recipe);
    }

    @PostMapping("/history/{historyId}/repeat/done")
    public ResponseEntity<?> repeatPreviousCook(@PathVariable Integer historyId, HttpSession session) {

        int result = userService.repeatPreviousCook((Integer) session.getAttribute("userId"), historyId);

        if (result == 0) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if (result == 3) {
            return ResponseEntity.status(400).body(new ApiResponse("Insufficient ingredient quantity"));
        }

        if (result == 4) {
            return ResponseEntity.status(400).body(new ApiResponse("Missing ingredient"));
        }

        if (result == 6) {
            return ResponseEntity.status(400).body(new ApiResponse("Cooking history not found"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("Previous recipe cooked successfully"));
    }

    @PostMapping("/system/{recipeId}/convert")
    public ResponseEntity<?> convertSystemRecipeToUserRecipe(@PathVariable Integer recipeId, HttpSession session) {

        int result = userService.convertSystemRecipeToUserRecipe((Integer) session.getAttribute("userId"), recipeId);

        if (result == 0) {
            return ResponseEntity.status(400).body(new ApiResponse("User not found"));
        }

        if (result == 6) {
            return ResponseEntity.status(400).body(new ApiResponse("System recipe not found"));
        }

        if (result == 3) {
            return ResponseEntity.status(400).body(new ApiResponse("Recipe name already exists for this user"));
        }

        return ResponseEntity.status(200).body(new ApiResponse("System recipe converted to user recipe successfully"));
    }
}
