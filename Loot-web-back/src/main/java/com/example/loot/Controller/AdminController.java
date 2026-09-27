package com.example.loot.Controller;

import com.example.loot.Repository.*;
import com.example.loot.Security.CurrentUser;
import com.example.loot.Api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserRepository users;
    private final IngredientRepository ingredients;
    private final SystemRecipeRepository recipes;
    private final UserRecipeRepository userRecipes;
    private final UserRecIngRepository userIngredients;
    private final PantryItemRepository pantry;
    private final CookingHistoryRepository history;
    private final CookingHisIngRepository historyIngredients;
    private final CurrentUser current;
    @GetMapping("/api/v1/admin/overview") public Map<String,Long> overview() { return Map.of("users",users.count(),"ingredients",ingredients.count(),"recipes",recipes.count(),"cooks",history.count()); }
    @DeleteMapping("/api/v1/user/delete/{id}") @Transactional
    public ApiResponse delete(@PathVariable Integer id) {
        if(current.id().equals(id))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cannot delete your active account");
        var u=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));
        for(var r:userRecipes.findUserRecipeByUserId(id)) { userIngredients.deleteAll(userIngredients.findUserRecIngByUserRecipeId(r.getId()));userRecipes.delete(r); }
        for(var h:history.findCookingHistoryByUserId(id)) { historyIngredients.deleteAll(historyIngredients.findCookingHisIngByCookingHistoryId(h.getId()));history.delete(h); }
        pantry.deleteAll(pantry.findAll().stream().filter(p->p.getUserId().equals(id)).toList());users.delete(u);return new ApiResponse("User deleted");
    }
}
