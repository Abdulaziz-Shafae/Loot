package com.example.loot.Service;

import com.example.loot.Model.SystemRecipe;
import com.example.loot.Repository.SystemRecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
//0 system recipe not found
//1 success
//2 system recipe name already taken

public class SystemRecipeService {
    private final com.example.loot.Repository.SystemRecIngRepository children;

    private final SystemRecipeRepository systemRecipeRepository;


    public List<SystemRecipe> getSystemRecipes(){
        return systemRecipeRepository.findAll();
    }


    public int addSystemRecipe(SystemRecipe systemRecipe){

        SystemRecipe oldSystemRecipe = systemRecipeRepository.findSystemRecipeByName(systemRecipe.getName());

        //name taken
        if(oldSystemRecipe != null){
            return 2;
        }

        systemRecipeRepository.save(systemRecipe);
        //added
        return 1;
    }


    public int editSystemRecipe(Integer id, SystemRecipe systemRecipe){

        SystemRecipe oldSystemRecipe = systemRecipeRepository.findSystemRecipeById(id);

        //system recipe not found
        if(oldSystemRecipe == null){
            return 0;
        }

        SystemRecipe nameCheck = systemRecipeRepository.findSystemRecipeByName(systemRecipe.getName());

        //name taken by another system recipe
        if(nameCheck != null && !nameCheck.getId().equals(id)){
            return 2;
        }

        oldSystemRecipe.setName(systemRecipe.getName());
        oldSystemRecipe.setDescription(systemRecipe.getDescription());
        oldSystemRecipe.setInstructions(systemRecipe.getInstructions());
        oldSystemRecipe.setCategory(systemRecipe.getCategory());
        oldSystemRecipe.setImageUrl(systemRecipe.getImageUrl());

        systemRecipeRepository.save(oldSystemRecipe);
        //updated
        return 1;
    }


    @org.springframework.transaction.annotation.Transactional
    public int deleteSystemRecipe(int id){

        SystemRecipe oldSystemRecipe = systemRecipeRepository.findSystemRecipeById(id);

        //system recipe not found
        if(oldSystemRecipe == null){
            return 0;
        }

        children.deleteAll(children.findSystemRecIngBySystemRecipeId(id));
        systemRecipeRepository.delete(oldSystemRecipe);
        //deleted
        return 1;
    }
}