package com.example.loot.Service;

import com.example.loot.Model.User;
import com.example.loot.Model.UserRecipe;
import com.example.loot.Repository.UserRepository;
import com.example.loot.Repository.UserRecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
//0 user recipe not found
//1 success
//2 user not found
//3 recipe name already exists for this user

public class UserRecipeService {
    private final com.example.loot.Repository.UserRecIngRepository children;
    private final com.example.loot.Security.CurrentUser currentUser;

    private final UserRecipeRepository userRecipeRepository;
    private final UserRepository userRepository;


    public List<UserRecipe> getUserRecipes(){
        return userRecipeRepository.findAll().stream().filter(x -> x.getUserId().equals(currentUser.id())).toList();
    }


    public int addUserRecipe(UserRecipe userRecipe){
        userRecipe.setId(null);
        userRecipe.setUserId(currentUser.id());

        User checkUser = userRepository.findUserById(userRecipe.getUserId());

        //user not found
        if(checkUser == null){
            return 2;
        }

        UserRecipe checkUserRecipe = userRecipeRepository.findUserRecipeByUserIdAndName(userRecipe.getUserId(), userRecipe.getName());

        //recipe name already exists for this user
        if(checkUserRecipe != null){
            return 3;
        }

        userRecipeRepository.save(userRecipe);
        //added
        return 1;
    }


    public int editUserRecipe(Integer id, UserRecipe userRecipe){

        UserRecipe oldUserRecipe = userRecipeRepository.findUserRecipeById(id);
        if (oldUserRecipe != null) currentUser.owns(oldUserRecipe.getUserId());

        //user recipe not found
        if(oldUserRecipe == null){
            return 0;
        }

        UserRecipe nameCheck = userRecipeRepository.findUserRecipeByUserIdAndName(oldUserRecipe.getUserId(), userRecipe.getName());

        //recipe name already taken by another recipe
        if(nameCheck != null && !nameCheck.getId().equals(id)){
            return 3;
        }

        oldUserRecipe.setName(userRecipe.getName());
        oldUserRecipe.setDescription(userRecipe.getDescription());
        oldUserRecipe.setInstructions(userRecipe.getInstructions());
        oldUserRecipe.setCategory(userRecipe.getCategory());
        oldUserRecipe.setImageUrl(userRecipe.getImageUrl());

        userRecipeRepository.save(oldUserRecipe);
        //updated
        return 1;
    }


    @org.springframework.transaction.annotation.Transactional
    public int deleteUserRecipe(int id){

        UserRecipe oldUserRecipe = userRecipeRepository.findUserRecipeById(id);
        if (oldUserRecipe != null) currentUser.owns(oldUserRecipe.getUserId());

        //user recipe not found
        if(oldUserRecipe == null){
            return 0;
        }

        children.deleteAll(children.findUserRecIngByUserRecipeId(id));
        userRecipeRepository.delete(oldUserRecipe);
        //deleted
        return 1;
    }
}