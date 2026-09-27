package com.example.loot.Service;

import com.example.loot.Model.Ingredient;
import com.example.loot.Model.UserRecIng;
import com.example.loot.Model.UserRecipe;
import com.example.loot.Repository.IngredientRepository;
import com.example.loot.Repository.UserRecIngRepository;
import com.example.loot.Repository.UserRecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
//0 user recipe ingredient not found
//1 success
//2 user recipe not found
//3 ingredient not found
//4 ingredient already exists in user recipe

public class UserRecIngService {
    private final com.example.loot.Security.CurrentUser currentUser;

    private final UserRecIngRepository userRecIngRepository;
    private final UserRecipeRepository userRecipeRepository;
    private final IngredientRepository ingredientRepository;


    public List<UserRecIng> getUserRecIng(){
        return userRecIngRepository.findAll().stream().filter(x -> currentUser.ownsUserRecipeQuietly(x.getUserRecipeId())).toList();
    }


    public int addUserRecIng(UserRecIng userRecIng){
        userRecIng.setId(null);
        currentUser.requireUserRecipe(userRecIng.getUserRecipeId());

        UserRecipe checkUserRecipe = userRecipeRepository.findUserRecipeById(userRecIng.getUserRecipeId());

        //user recipe not found
        if(checkUserRecipe == null){
            return 2;
        }

        Ingredient checkIngredient = ingredientRepository.findIngredientById(userRecIng.getIngredientId());

        //ingredient not found
        if(checkIngredient == null){
            return 3;
        }

        UserRecIng checkUserRecIng = userRecIngRepository.findUserRecIngByUserRecipeIdAndIngredientId(userRecIng.getUserRecipeId(), userRecIng.getIngredientId());

        //ingredient already exists in user recipe
        if(checkUserRecIng != null){
            return 4;
        }

        userRecIngRepository.save(userRecIng);
        //added
        return 1;
    }


    public int editUserRecIng(Integer id, UserRecIng userRecIng){

        UserRecIng oldUserRecIng = userRecIngRepository.findUserRecIngById(id);
        if (oldUserRecIng != null) currentUser.requireUserRecipe(oldUserRecIng.getUserRecipeId());

        //user recipe ingredient not found
        if(oldUserRecIng == null){
            return 0;
        }

        oldUserRecIng.setRequiredQuantity(userRecIng.getRequiredQuantity());

        userRecIngRepository.save(oldUserRecIng);
        //updated
        return 1;
    }


    public int deleteUserRecIng(int id){

        UserRecIng oldUserRecIng = userRecIngRepository.findUserRecIngById(id);
        if (oldUserRecIng != null) currentUser.requireUserRecipe(oldUserRecIng.getUserRecipeId());

        //user recipe ingredient not found
        if(oldUserRecIng == null){
            return 0;
        }

        userRecIngRepository.delete(oldUserRecIng);
        //deleted
        return 1;
    }
}