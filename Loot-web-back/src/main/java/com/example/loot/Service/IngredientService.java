package com.example.loot.Service;

import com.example.loot.Model.Ingredient;
import com.example.loot.Repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
//0 ingredient not found
//1 success
//2 ingredient name already taken

    public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final com.example.loot.Repository.PantryItemRepository pantry;
    private final com.example.loot.Repository.SystemRecIngRepository systemIngredients;
    private final com.example.loot.Repository.UserRecIngRepository userIngredients;
    private final com.example.loot.Repository.CookingHisIngRepository historyIngredients;
    private void requireUnused(Integer id) {
        if(pantry.findAll().stream().anyMatch(x->x.getIngredientId().equals(id)) || systemIngredients.findAll().stream().anyMatch(x->x.getIngredientId().equals(id)) || userIngredients.findAll().stream().anyMatch(x->x.getIngredientId().equals(id)) || historyIngredients.findAll().stream().anyMatch(x->x.getIngredientId().equals(id)))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Ingredient is in use");
    }


    public List<Ingredient> getIngredients(){
        return ingredientRepository.findAll();
    }


    public int addIngredient(Ingredient ingredient){

        Ingredient oldIngredient = ingredientRepository.findIngredientByName(ingredient.getName());

        //name taken
        if(oldIngredient != null){
            return 2;
        }

        ingredient.setId(null);
        ingredientRepository.save(ingredient);
        //added
        return 1;
    }


    public int editIngredient(Integer id, Ingredient ingredient){

        Ingredient oldIngredient = ingredientRepository.findIngredientById(id);

        //ingredient not found
        if(oldIngredient == null){
            return 0;
        }

        //name taken by another ingredient
        Ingredient nameCheck = ingredientRepository.findIngredientByName(ingredient.getName());

        if(nameCheck != null && !nameCheck.getId().equals(id)){
            return 2;
        }

        oldIngredient.setName(ingredient.getName());
        if(!oldIngredient.getUnit().equals(ingredient.getUnit())) requireUnused(id);
        oldIngredient.setUnit(ingredient.getUnit());

        ingredientRepository.save(oldIngredient);
        //updated
        return 1;
    }


    public int deleteIngredient(int id){

        Ingredient oldIngredient = ingredientRepository.findIngredientById(id);

        //ingredient not found
        if(oldIngredient == null){
            return 0;
        }

        requireUnused(id);
        ingredientRepository.delete(oldIngredient);
        //deleted
        return 1;
    }
}
