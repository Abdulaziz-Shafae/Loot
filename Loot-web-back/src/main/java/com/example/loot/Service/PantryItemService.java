package com.example.loot.Service;

import com.example.loot.Model.Ingredient;
import com.example.loot.Model.PantryItem;
import com.example.loot.Model.User;
import com.example.loot.Repository.IngredientRepository;
import com.example.loot.Repository.PantryItemRepository;
import com.example.loot.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
//0 pantry item not found
//1 success
//2 user not found
//3 ingredient not found
//4 ingredient already exists in user's pantry

public class PantryItemService {
    private final com.example.loot.Security.CurrentUser currentUser;

    private final PantryItemRepository pantryItemRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;


    public List<PantryItem> getPantryItems(){
        return pantryItemRepository.findAll().stream().filter(x -> x.getUserId().equals(currentUser.id())).toList();
    }


    public int addPantryItem(PantryItem pantryItem){
        pantryItem.setId(null);
        pantryItem.setUserId(currentUser.id());

        User checkUser = userRepository.findUserById(pantryItem.getUserId());

        if(checkUser==null){
            return 2;
        }

        Ingredient checkIngredient = ingredientRepository.findIngredientById(pantryItem.getIngredientId());

        if(checkIngredient==null){
            return 3;
        }

        PantryItem checkPantryItem = pantryItemRepository.findPantryItemByUserIdAndIngredientId(pantryItem.getUserId(), pantryItem.getIngredientId());

        //ingredient already exists in user's pantry
        if(checkPantryItem != null){
            return 4;
        }

        pantryItemRepository.save(pantryItem);
        //added
        return 1;
    }


    public int editPantryItem(Integer id, PantryItem pantryItem){

        PantryItem oldPantryItem = pantryItemRepository.findPantryItemById(id);
        if (oldPantryItem != null) currentUser.owns(oldPantryItem.getUserId());

        //pantry item not found
        if(oldPantryItem == null){
            return 0;
        }

        oldPantryItem.setQuantity(pantryItem.getQuantity());
        oldPantryItem.setLowStockThreshold(pantryItem.getLowStockThreshold());

        pantryItemRepository.save(oldPantryItem);
        //updated
        return 1;
    }


    public int deletePantryItem(int id){

        PantryItem oldPantryItem = pantryItemRepository.findPantryItemById(id);
        if (oldPantryItem != null) currentUser.owns(oldPantryItem.getUserId());

        //pantry item not found
        if(oldPantryItem == null){
            return 0;
        }

        pantryItemRepository.delete(oldPantryItem);
        //deleted
        return 1;
    }
}