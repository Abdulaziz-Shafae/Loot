package com.example.loot.Service;

import com.example.loot.DTO.*;
import com.example.loot.Model.*;
import com.example.loot.Repository.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor

public class UserService {

    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;
    private final PantryItemRepository pantryItemRepository;

    private final UserRecipeRepository userRecipeRepository;
    private final UserRecIngRepository userRecIngRepository;
    private final SystemRecipeRepository systemRecipeRepository;
    private final SystemRecIngRepository systemRecIngRepository;
    private final CookingHistoryRepository cookingHistoryRepository;
    private final CookingHisIngRepository cookingHisIngRepository;

    private final EmailService emailService;

    public int haveTheIngredient(Integer userId ,Integer ingredientId , Double reqQun){

        PantryItem item = pantryItemRepository.findPantryItemByUserIdAndIngredientId(userId,ingredientId);

        if (item == null) {
            return 4;
        }

        if( (item.getQuantity() - reqQun ) >= 0 ){
            return 1;
        }

        return 3;
    }

    public int canCookRecipe(Integer userId, Integer recipeId , String listType){
        User oldUser = userRepository.findUserById(userId);

        //user not found
        if(oldUser == null){
            return 0;
        }

        if( listType.equalsIgnoreCase("user")) {
            UserRecipe userRecipe = userRecipeRepository.findUserRecipeByIdAndUserId(recipeId, userId);

            if (userRecipe != null) {
                List<UserRecIng> userRecIng = userRecIngRepository.findUserRecIngByUserRecipeId(recipeId);
                if (userRecIng.isEmpty()) return 6;
                for (int i = 0; i < userRecIng.size(); i++) {
                    int result= haveTheIngredient(userId, userRecIng.get(i).getIngredientId(), userRecIng.get(i).getRequiredQuantity());
                    if (result == 3) {
                        //insufficient ingredient
                        return 3;
                    }else if( result == 4){
                        //missing ingredient
                        return 4;
                    }
                }

                return 1;
            }

            //invalid recipe
            return 6;

        }else if(listType.equalsIgnoreCase("system")) {
            SystemRecipe systemRecipe = systemRecipeRepository.findSystemRecipeById(recipeId);

            if (systemRecipe != null) {
                List<SystemRecIng> systemRecIngs = systemRecIngRepository.findSystemRecIngBySystemRecipeId(recipeId);
                if (systemRecIngs.isEmpty()) return 6;
                for (int i = 0; i < systemRecIngs.size(); i++) {
                    int result= haveTheIngredient(userId, systemRecIngs.get(i).getIngredientId(), systemRecIngs.get(i).getRequiredQuantity());
                    if (result == 3) {
                        //insufficient ingredient
                        return 3;
                    }else if( result == 4){
                        //missing ingredient
                        return 4;
                    }
                }

                return 1;
            }

            //invalid recipe
            return 6;
        }
        //System or user
        return 5;

    }

    public List<MissingIngredientDTO> missingList(Integer userId, Integer recipeId , String listType){
        User oldUser = userRepository.findUserById(userId);

        //user not found
        if(oldUser == null){
            return null;
        }


        List<MissingIngredientDTO> list= new ArrayList<>();

        if( listType.equalsIgnoreCase("user")) {
            UserRecipe userRecipe = userRecipeRepository.findUserRecipeByIdAndUserId(recipeId, userId);

            if (userRecipe != null) {
                List<UserRecIng> userRecIng = userRecIngRepository.findUserRecIngByUserRecipeId(recipeId);
                for (int i = 0; i < userRecIng.size(); i++) {
                    Ingredient ingredient = ingredientRepository.findIngredientById(userRecIng.get(i).getIngredientId());
                    PantryItem pantryItem = pantryItemRepository.findPantryItemByUserIdAndIngredientId(userId , ingredient.getId());
                    int result= haveTheIngredient(userId, userRecIng.get(i).getIngredientId(), userRecIng.get(i).getRequiredQuantity());

                    if (result == 3) {
                        //insufficient ingredient
                        MissingIngredientDTO missingIngredientDTO=
                                new MissingIngredientDTO( ingredient.getName() ,userRecIng.get(i).getRequiredQuantity() ,pantryItem.getQuantity() ,userRecIng.get(i).getRequiredQuantity() - pantryItem.getQuantity() );

                        list.add(missingIngredientDTO);
                    }else if( result == 4){
                        //missing ingredient
                        MissingIngredientDTO missingIngredientDTO=
                                new MissingIngredientDTO(ingredient.getName() ,userRecIng.get(i).getRequiredQuantity() , 0.0 ,userRecIng.get(i).getRequiredQuantity());

                        list.add(missingIngredientDTO);
                    }
                }
                return list;
            }

            return null;

        }
        else if(listType.equalsIgnoreCase("system")) {
            SystemRecipe systemRecipe = systemRecipeRepository.findSystemRecipeById(recipeId);

            if (systemRecipe != null) {
                List<SystemRecIng> systemRecIngs = systemRecIngRepository.findSystemRecIngBySystemRecipeId(recipeId);
                for (int i = 0; i < systemRecIngs.size(); i++) {
                    Ingredient ingredient = ingredientRepository.findIngredientById(systemRecIngs.get(i).getIngredientId());
                    PantryItem pantryItem = pantryItemRepository.findPantryItemByUserIdAndIngredientId(userId , ingredient.getId());
                    int result= haveTheIngredient(userId, systemRecIngs.get(i).getIngredientId(), systemRecIngs.get(i).getRequiredQuantity());
                    if (result == 3) {
                        //insufficient ingredient
                        MissingIngredientDTO missingIngredientDTO=
                                new MissingIngredientDTO(ingredient.getName() ,systemRecIngs.get(i).getRequiredQuantity() ,pantryItem.getQuantity() ,systemRecIngs.get(i).getRequiredQuantity() - pantryItem.getQuantity() );

                        list.add(missingIngredientDTO);

                    }else if( result == 4){
                        //missing ingredient
                        MissingIngredientDTO missingIngredientDTO=
                                new MissingIngredientDTO(ingredient.getName() ,systemRecIngs.get(i).getRequiredQuantity() , 0.0 ,systemRecIngs.get(i).getRequiredQuantity());

                        list.add(missingIngredientDTO);
                    }
                }
                return list;
            }

            return null;
        }
        //System or user
        return null;

    }

    public List<RecipesDTO> getSystemRecipes() {

        List<SystemRecipe> systemRecipes = systemRecipeRepository.findAll();

        List<RecipesDTO> recipes = new ArrayList<>();

        for (int i = 0; i < systemRecipes.size(); i++) {

            List<SystemRecIng> systemRecIngs = systemRecIngRepository.findSystemRecIngBySystemRecipeId(systemRecipes.get(i).getId());

            List<String> ingredientList = new ArrayList<>();

            for (int j = 0; j < systemRecIngs.size(); j++) {

                Ingredient ingredient = ingredientRepository.findIngredientById(systemRecIngs.get(j).getIngredientId());

                ingredientList.add(ingredient.getName() + " - " + systemRecIngs.get(j).getRequiredQuantity() + " " + ingredient.getUnit());
            }

            RecipesDTO recipe = new RecipesDTO(systemRecipes.get(i).getName(), systemRecipes.get(i).getDescription(), systemRecipes.get(i).getInstructions(), systemRecipes.get(i).getCategory(), ingredientList);

            recipes.add(recipe);
        }

        return recipes;
    }

    public List<RecipesDTO> getSystemRecipesByCategory(String category) {

        List<SystemRecipe> systemRecipes = systemRecipeRepository.findSystemRecipeByCategory(category);

        List<RecipesDTO> recipes = new ArrayList<>();

        for (int i = 0; i < systemRecipes.size(); i++) {

            List<SystemRecIng> systemRecIngs = systemRecIngRepository.findSystemRecIngBySystemRecipeId(systemRecipes.get(i).getId());

            List<String> ingredientList = new ArrayList<>();

            for (int j = 0; j < systemRecIngs.size(); j++) {

                Ingredient ingredient = ingredientRepository.findIngredientById(systemRecIngs.get(j).getIngredientId());

                ingredientList.add(ingredient.getName() + " - " + systemRecIngs.get(j).getRequiredQuantity() + " " + ingredient.getUnit());
            }

            RecipesDTO recipe = new RecipesDTO(systemRecipes.get(i).getName(), systemRecipes.get(i).getDescription(), systemRecipes.get(i).getInstructions(), systemRecipes.get(i).getCategory(), ingredientList);

            recipes.add(recipe);
        }

        return recipes;
    }

    public List<RecipesDTO> systemRecipesByCategory(Integer userId , String category){
        User oldUser = userRepository.findUserById(userId);

        //user not found
        if(oldUser == null){
            return null;
        }

        List<SystemRecipe> systemRecipes = systemRecipeRepository.findSystemRecipeByCategory(category);

        List<RecipesDTO> list= new ArrayList<>();

        for(int i=0 ; i< systemRecipes.size() ; i++){
            if( canCookRecipe(userId, systemRecipes.get(i).getId(), "system")== 1 ){

                List<String> ingredientList = new ArrayList<>();
                List<SystemRecIng> systemRecIngs = systemRecIngRepository.findSystemRecIngBySystemRecipeId(systemRecipes.get(i).getId());

                for (int j = 0; j < systemRecIngs.size(); j++) {

                    Ingredient ingredient = ingredientRepository.findIngredientById(systemRecIngs.get(j).getIngredientId());

                    ingredientList.add(ingredient.getName());
                }

                RecipesDTO recipe = new RecipesDTO(systemRecipes.get(i).getName(), systemRecipes.get(i).getDescription(), systemRecipes.get(i).getInstructions(), systemRecipes.get(i).getCategory(), ingredientList);
                list.add(recipe);

            }
        }
        return list;
    }

    public List<AlmostRecipeDTO> systemRecipesByCategoryAlmost(Integer userId , String category){
        User oldUser = userRepository.findUserById(userId);

        //user not found
        if(oldUser == null){
            return null;
        }

        List<SystemRecipe> systemRecipes = systemRecipeRepository.findSystemRecipeByCategory(category);

        List<AlmostRecipeDTO> list= new ArrayList<>();

        for(int i=0 ; i< systemRecipes.size() ; i++){
            List<MissingIngredientDTO> missing = missingList(userId, systemRecipes.get(i).getId(), "system");

            if (missing != null && missing.size() >= 1 && missing.size() <= 3) {

                List<String> ingredientList = new ArrayList<>();
                List<SystemRecIng> systemRecIngs = systemRecIngRepository.findSystemRecIngBySystemRecipeId(systemRecipes.get(i).getId());

                for (int j = 0; j < systemRecIngs.size(); j++) {

                    Ingredient ingredient = ingredientRepository.findIngredientById(systemRecIngs.get(j).getIngredientId());

                    ingredientList.add(ingredient.getName());
                }

                AlmostRecipeDTO recipe = new AlmostRecipeDTO(systemRecipes.get(i).getName(), systemRecipes.get(i).getDescription(), systemRecipes.get(i).getInstructions(),systemRecipes.get(i).getCategory(), ingredientList, missing);
                list.add(recipe);

            }
        }
        return list;
    }

    public List<RecipesDTO> getUserRecipes(Integer userId) {

        User oldUser = userRepository.findUserById(userId);

        // user not found
        if (oldUser == null) {
            return null;
        }

        List<UserRecipe> userRecipes = userRecipeRepository.findUserRecipeByUserId(userId);

        List<RecipesDTO> recipes = new ArrayList<>();

        for (int i = 0; i < userRecipes.size(); i++) {

            List<UserRecIng> userRecIngs = userRecIngRepository.findUserRecIngByUserRecipeId(userRecipes.get(i).getId());

            List<String> ingredientList = new ArrayList<>();

            for (int j = 0; j < userRecIngs.size(); j++) {

                Ingredient ingredient = ingredientRepository.findIngredientById(userRecIngs.get(j).getIngredientId());

                ingredientList.add(ingredient.getName() + " - " + userRecIngs.get(j).getRequiredQuantity() + " " + ingredient.getUnit());
            }

            RecipesDTO recipe = new RecipesDTO(userRecipes.get(i).getName(), userRecipes.get(i).getDescription(), userRecipes.get(i).getInstructions(), userRecipes.get(i).getCategory(), ingredientList);
            recipes.add(recipe);
        }

        return recipes;
    }

    public List<RecipesDTO> getUserRecipesByCategory(Integer userId, String category) {

        User oldUser = userRepository.findUserById(userId);

        // user not found
        if (oldUser == null) {
            return null;
        }

        List<UserRecipe> userRecipes = userRecipeRepository.findUserRecipeByUserIdAndCategory(userId, category);

        List<RecipesDTO> recipes = new ArrayList<>();

        for (int i = 0; i < userRecipes.size(); i++) {

            List<UserRecIng> userRecIngs = userRecIngRepository.findUserRecIngByUserRecipeId(userRecipes.get(i).getId());

            List<String> ingredientList = new ArrayList<>();

            for (int j = 0; j < userRecIngs.size(); j++) {

                Ingredient ingredient = ingredientRepository.findIngredientById(userRecIngs.get(j).getIngredientId());

                ingredientList.add(ingredient.getName() + " - " + userRecIngs.get(j).getRequiredQuantity() + " " + ingredient.getUnit());
            }

            RecipesDTO recipe = new RecipesDTO(userRecipes.get(i).getName(), userRecipes.get(i).getDescription(), userRecipes.get(i).getInstructions(), userRecipes.get(i).getCategory(), ingredientList);
            recipes.add(recipe);
        }

        return recipes;
    }

    public List<RecipesDTO> userRecipesByCategory(Integer userId , String category){
        User oldUser = userRepository.findUserById(userId);

        //user not found
        if(oldUser == null){
            return null;
        }

        List<UserRecipe> userRecipes = userRecipeRepository.findUserRecipeByUserIdAndCategory(userId, category);

        List<RecipesDTO> list= new ArrayList<>();

        for(int i=0 ; i< userRecipes.size() ; i++){
            if( canCookRecipe(userId, userRecipes.get(i).getId(), "user")== 1 ){

                List<String> ingredientList = new ArrayList<>();
                List<UserRecIng> userRecIngs = userRecIngRepository.findUserRecIngByUserRecipeId(userRecipes.get(i).getId());

                for (int j = 0; j < userRecIngs.size(); j++) {

                    Ingredient ingredient = ingredientRepository.findIngredientById(userRecIngs.get(j).getIngredientId());

                    ingredientList.add(ingredient.getName());
                }

                RecipesDTO recipe = new RecipesDTO(userRecipes.get(i).getName(), userRecipes.get(i).getDescription(), userRecipes.get(i).getInstructions(), userRecipes.get(i).getCategory(), ingredientList);
                list.add(recipe);

            }
        }
        return list;
    }

    public List<AlmostRecipeDTO> userRecipesByCategoryAlmost(Integer userId , String category){
        User oldUser = userRepository.findUserById(userId);

        //user not found
        if(oldUser == null){
            return null;
        }

        List<UserRecipe> userRecipes = userRecipeRepository.findUserRecipeByUserIdAndCategory(userId, category);
        List<AlmostRecipeDTO> list= new ArrayList<>();

        for(int i=0 ; i< userRecipes.size() ; i++){
            List<MissingIngredientDTO> missing = missingList(userId, userRecipes.get(i).getId(), "user");

            if (missing != null && missing.size() >= 1 && missing.size() <= 3) {

                List<String> ingredientList = new ArrayList<>();
                List<UserRecIng> userRecIngs = userRecIngRepository.findUserRecIngByUserRecipeId(userRecipes.get(i).getId());

                for (int j = 0; j < userRecIngs.size(); j++) {

                    Ingredient ingredient = ingredientRepository.findIngredientById(userRecIngs.get(j).getIngredientId());

                    ingredientList.add(ingredient.getName());
                }

                AlmostRecipeDTO recipe = new AlmostRecipeDTO(userRecipes.get(i).getName(), userRecipes.get(i).getDescription(), userRecipes.get(i).getInstructions(),userRecipes.get(i).getCategory(), ingredientList, missing);
                list.add(recipe);

            }
        }
        return list;
    }

    public List<LowStockDTO> lowStock(Integer userId){
        User oldUser = userRepository.findUserById(userId);

        //user not found
        if(oldUser == null){
            return null;
        }

        List<PantryItem> lowStockItems = pantryItemRepository.findLowStockByUserId(userId);
        List<LowStockDTO> list = new ArrayList<>();

        for(int i=0 ; i<lowStockItems.size() ; i++){
            Ingredient ingredient = ingredientRepository.findIngredientById(lowStockItems.get(i).getIngredientId());
            LowStockDTO lowStockDTO=new LowStockDTO(ingredient.getName(), lowStockItems.get(i).getQuantity() +" "+ingredient.getUnit(), lowStockItems.get(i).getLowStockThreshold()+" "+ingredient.getUnit() , (lowStockItems.get(i).getLowStockThreshold()-lowStockItems.get(i).getQuantity())+" "+ingredient.getUnit());

            list.add(lowStockDTO);
        }

        return list;

    }

    public int lowStockMSG(Integer userID){

        User oldUser = userRepository.findUserById(userID);

        // user not found
        if(oldUser == null){
            return 0;
        }

        List<LowStockDTO> list = lowStock(userID);

        // no low stock ingredients
        if(list.isEmpty()){
            return 2;
        }

        // send the list by email
        emailService.sendLowStockEmail(oldUser.getEmail(), oldUser.getName(), list);

        // sent
        return 1;
    }

    public void removeIngredient(Integer userId ,Integer ingredientId , Double reqQun) {

        PantryItem item = pantryItemRepository.findPantryItemByUserIdAndIngredientId(userId, ingredientId);

        item.setQuantity(item.getQuantity() - reqQun);

        pantryItemRepository.save(item);

    }

    public void addCookingHistory(Integer userId, Integer recipeId, String listType) {

        CookingHistory cookingHistory = new CookingHistory();

        cookingHistory.setUserId(userId);
        cookingHistory.setRecipeId(recipeId);
        cookingHistory.setCookedAt(LocalDateTime.now());


        if (listType.equalsIgnoreCase("user")) {

            UserRecipe userRecipe = userRecipeRepository.findUserRecipeByIdAndUserId(recipeId, userId);

            cookingHistory.setRecipeName(userRecipe.getName());
            cookingHistory.setDescription(userRecipe.getDescription());
            cookingHistory.setInstructions(userRecipe.getInstructions());
            cookingHistory.setCategory(userRecipe.getCategory());
            cookingHistory.setRecipeType("User");

            cookingHistoryRepository.save(cookingHistory);


            List<UserRecIng> userRecIngs = userRecIngRepository.findUserRecIngByUserRecipeId(recipeId);

            for (int i = 0; i < userRecIngs.size(); i++) {

                Ingredient ingredient = ingredientRepository.findIngredientById(userRecIngs.get(i).getIngredientId());

                CookingHisIng cookingHisIng = new CookingHisIng();

                cookingHisIng.setCookingHistoryId(cookingHistory.getId());
                cookingHisIng.setIngredientId(ingredient.getId());
                cookingHisIng.setIngredientName(ingredient.getName());
                cookingHisIng.setUsedQuantity(userRecIngs.get(i).getRequiredQuantity());
                cookingHisIng.setUnit(ingredient.getUnit());

                cookingHisIngRepository.save(cookingHisIng);
            }


        } else if (listType.equalsIgnoreCase("system")) {

            SystemRecipe systemRecipe = systemRecipeRepository.findSystemRecipeById(recipeId);

            cookingHistory.setRecipeName(systemRecipe.getName());
            cookingHistory.setDescription(systemRecipe.getDescription());
            cookingHistory.setInstructions(systemRecipe.getInstructions());
            cookingHistory.setCategory(systemRecipe.getCategory());
            cookingHistory.setRecipeType("System");

            cookingHistoryRepository.save(cookingHistory);


            List<SystemRecIng> systemRecIngs = systemRecIngRepository.findSystemRecIngBySystemRecipeId(recipeId);

            for (int i = 0; i < systemRecIngs.size(); i++) {

                Ingredient ingredient = ingredientRepository.findIngredientById(systemRecIngs.get(i).getIngredientId());

                CookingHisIng cookingHisIng = new CookingHisIng();

                cookingHisIng.setCookingHistoryId(cookingHistory.getId());
                cookingHisIng.setIngredientId(ingredient.getId());
                cookingHisIng.setIngredientName(ingredient.getName());
                cookingHisIng.setUsedQuantity(systemRecIngs.get(i).getRequiredQuantity());
                cookingHisIng.setUnit(ingredient.getUnit());

                cookingHisIngRepository.save(cookingHisIng);
            }
        }
    }

    @Transactional
    public int cookRecipe(Integer userId, Integer recipeId , String listType){
        User oldUser = userRepository.findUserById(userId);

        //user not found
        if(oldUser == null){
            return 0;
        }
        int result = canCookRecipe(userId, recipeId, listType);

        if(result==1) {

            if (listType.equalsIgnoreCase("user")) {

                UserRecipe userRecipe = userRecipeRepository.findUserRecipeByIdAndUserId(recipeId, userId);

                if (userRecipe != null) {
                    List<UserRecIng> userRecIng = userRecIngRepository.findUserRecIngByUserRecipeId(recipeId);
                    for (int i = 0; i < userRecIng.size(); i++) {
                        removeIngredient(userId, userRecIng.get(i).getIngredientId(), userRecIng.get(i).getRequiredQuantity());
                    }

                    addCookingHistory(userId, recipeId, listType);

                    return 1;
                }

                //invalid recipe
                return 6;

            } else if (listType.equalsIgnoreCase("system")) {
                SystemRecipe systemRecipe = systemRecipeRepository.findSystemRecipeById(recipeId);

                if (systemRecipe != null) {
                    List<SystemRecIng> systemRecIngs = systemRecIngRepository.findSystemRecIngBySystemRecipeId(recipeId);
                    for (int i = 0; i < systemRecIngs.size(); i++) {
                        removeIngredient(userId, systemRecIngs.get(i).getIngredientId(), systemRecIngs.get(i).getRequiredQuantity());
                    }

                    addCookingHistory(userId, recipeId, listType);

                    return 1;
                }

                //invalid recipe
                return 6;
            }
        }

        return result;

    }

    public CookingDTO getRecipeForCooking(Integer userId, Integer recipeId, String listType) {

        User user = userRepository.findUserById(userId);

        // User not found
        if (user == null) {
            return null;
        }

        List<String> ingredients = new ArrayList<>();


        if (listType.equalsIgnoreCase("user")) {

            UserRecipe userRecipe = userRecipeRepository.findUserRecipeByIdAndUserId(recipeId, userId);

            // Recipe not found
            if (userRecipe == null) {
                return null;
            }

            List<UserRecIng> userRecIngs = userRecIngRepository.findUserRecIngByUserRecipeId(recipeId);

            for (int i = 0; i < userRecIngs.size(); i++) {

                Ingredient ingredient = ingredientRepository.findIngredientById(userRecIngs.get(i).getIngredientId());

                ingredients.add(ingredient.getName() + " - " + userRecIngs.get(i).getRequiredQuantity() + " " + ingredient.getUnit());
            }

            List<MissingIngredientDTO> missing = missingList(userId, recipeId, "user");

            return new CookingDTO(userRecipe.getName(), userRecipe.getDescription(), userRecipe.getInstructions(), userRecipe.getCategory(), ingredients, missing);

        } else if (listType.equalsIgnoreCase("system")) {

            SystemRecipe systemRecipe = systemRecipeRepository.findSystemRecipeById(recipeId);

            // Recipe not found
            if (systemRecipe == null) {
                return null;
            }

            List<SystemRecIng> systemRecIngs = systemRecIngRepository.findSystemRecIngBySystemRecipeId(recipeId);

            for (int i = 0; i < systemRecIngs.size(); i++) {

                Ingredient ingredient = ingredientRepository.findIngredientById(systemRecIngs.get(i).getIngredientId());

                ingredients.add(ingredient.getName() + " - " + systemRecIngs.get(i).getRequiredQuantity() + " " + ingredient.getUnit());
            }

            List<MissingIngredientDTO> missing =
                    missingList(userId, recipeId, "system");

            return new CookingDTO(systemRecipe.getName(), systemRecipe.getDescription(), systemRecipe.getInstructions(), systemRecipe.getCategory(), ingredients, missing);
        }

        // Invalid listType
        return null;
    }

    public List<RecipesDTO> getHistoryByUserId(Integer userId) {

        User oldUser = userRepository.findUserById(userId);

        // user not found
        if (oldUser == null) {
            return null;
        }

        List<CookingHistory> cookingHistories = cookingHistoryRepository.findCookingHistoryByUserId(userId);

        List<RecipesDTO> recipes = new ArrayList<>();

        for (int i = 0; i < cookingHistories.size(); i++) {

            CookingHistory history = cookingHistories.get(i);

            List<CookingHisIng> cookingHisIngs = cookingHisIngRepository.findCookingHisIngByCookingHistoryId(history.getId());

            List<String> ingredients = new ArrayList<>();

            for (int j = 0; j < cookingHisIngs.size(); j++) {

                CookingHisIng ingredient = cookingHisIngs.get(j);

                ingredients.add(ingredient.getIngredientName() + " - " + ingredient.getUsedQuantity() + " " + ingredient.getUnit());
            }

            RecipesDTO recipe = new RecipesDTO(history.getRecipeName(), history.getDescription(), history.getInstructions(), history.getCategory(), ingredients);

            recipes.add(recipe);
        }

        return recipes;
    }

    public List<RecipesDTO> getHistoryByCategory(Integer userId, String category) {

        User oldUser = userRepository.findUserById(userId);

        // user not found
        if (oldUser == null) {
            return null;
        }

        List<CookingHistory> cookingHistories = cookingHistoryRepository.findCookingHistoryByUserIdAndCategoryIgnoreCase(userId, category);

        List<RecipesDTO> recipes = new ArrayList<>();

        for (int i = 0; i < cookingHistories.size(); i++) {

            CookingHistory history = cookingHistories.get(i);

            List<CookingHisIng> cookingHisIngs = cookingHisIngRepository.findCookingHisIngByCookingHistoryId(history.getId());

            List<String> ingredients = new ArrayList<>();

            for (int j = 0; j < cookingHisIngs.size(); j++) {

                CookingHisIng ingredient = cookingHisIngs.get(j);

                ingredients.add(ingredient.getIngredientName() + " - " + ingredient.getUsedQuantity() + " " + ingredient.getUnit());
            }

            recipes.add(
                    new RecipesDTO(history.getRecipeName(), history.getDescription(), history.getInstructions(), history.getCategory(), ingredients));
        }

        return recipes;
    }

    public List<RecipesDTO> possibleHistoryByCategory(Integer userId, String category) {

        User oldUser = userRepository.findUserById(userId);

        // user not found
        if (oldUser == null) {
            return null;
        }

        List<CookingHistory> cookingHistories = cookingHistoryRepository.findCookingHistoryByUserIdAndCategoryIgnoreCase(userId, category);

        List<RecipesDTO> recipes = new ArrayList<>();

        for (int i = 0; i < cookingHistories.size(); i++) {

            CookingHistory history = cookingHistories.get(i);

            List<CookingHisIng> cookingHisIngs = cookingHisIngRepository.findCookingHisIngByCookingHistoryId(history.getId());

            boolean canCook = true;

            List<String> ingredients = new ArrayList<>();

            for (int j = 0; j < cookingHisIngs.size(); j++) {

                CookingHisIng historyIngredient = cookingHisIngs.get(j);

                int result = haveTheIngredient(userId, historyIngredient.getIngredientId(), historyIngredient.getUsedQuantity());

                if (result != 1) {
                    canCook = false;
                    break;
                }

                ingredients.add(historyIngredient.getIngredientName() + " - " + historyIngredient.getUsedQuantity() + " " + historyIngredient.getUnit());
            }

            if (canCook) {
                recipes.add(new RecipesDTO(history.getRecipeName(), history.getDescription(), history.getInstructions(), history.getCategory(), ingredients));
            }
        }

        return recipes;
    }

    public List<AlmostRecipeDTO> almostHistoryByCategory(Integer userId, String category) {

        User oldUser = userRepository.findUserById(userId);

        // user not found
        if (oldUser == null) {
            return null;
        }

        List<CookingHistory> cookingHistories = cookingHistoryRepository.findCookingHistoryByUserIdAndCategoryIgnoreCase(userId, category);

        List<AlmostRecipeDTO> recipes = new ArrayList<>();


        for (int i = 0; i < cookingHistories.size(); i++) {

            CookingHistory history = cookingHistories.get(i);

            List<CookingHisIng> cookingHisIngs = cookingHisIngRepository.findCookingHisIngByCookingHistoryId(history.getId());

            List<String> ingredients = new ArrayList<>();
            List<MissingIngredientDTO> missing = new ArrayList<>();


            for (int j = 0; j < cookingHisIngs.size(); j++) {

                CookingHisIng historyIngredient = cookingHisIngs.get(j);

                ingredients.add(historyIngredient.getIngredientName() + " - " + historyIngredient.getUsedQuantity() + " " + historyIngredient.getUnit());


                PantryItem pantryItem = pantryItemRepository.findPantryItemByUserIdAndIngredientId(userId, historyIngredient.getIngredientId());


                // Ingredient completely missing
                if (pantryItem == null) {

                    missing.add(new MissingIngredientDTO(historyIngredient.getIngredientName(), historyIngredient.getUsedQuantity(), 0.0, historyIngredient.getUsedQuantity()));

                }

                // Ingredient exists but quantity is insufficient
                else if (pantryItem.getQuantity() < historyIngredient.getUsedQuantity()) {

                    Double missingQuantity = historyIngredient.getUsedQuantity() - pantryItem.getQuantity();

                    missing.add(new MissingIngredientDTO(historyIngredient.getIngredientName(), historyIngredient.getUsedQuantity(), pantryItem.getQuantity(), missingQuantity));
                }
            }


            // Almost possible = missing 1 to 3 ingredients
            if (missing != null && missing.size() >= 1 && missing.size() <= 3) {

                recipes.add(new AlmostRecipeDTO(history.getRecipeName(), history.getDescription(), history.getInstructions(), history.getCategory(),ingredients, missing));
            }
        }

        return recipes;
    }

    public CookingDTO getPreviousCook(Integer userId, Integer historyId) {

        User user = userRepository.findUserById(userId);

        // user not found
        if (user == null) {
            return null;
        }

        CookingHistory history = cookingHistoryRepository.findCookingHistoryByIdAndUserId(historyId, userId);

        // history not found or does not belong to user
        if (history == null) {
            return null;
        }

        List<CookingHisIng> historyIngredients = cookingHisIngRepository.findCookingHisIngByCookingHistoryId(historyId);

        List<String> ingredients = new ArrayList<>();
        List<MissingIngredientDTO> missing = new ArrayList<>();

        for (int i = 0; i < historyIngredients.size(); i++) {

            CookingHisIng historyIngredient = historyIngredients.get(i);

            ingredients.add(historyIngredient.getIngredientName() + " - " + historyIngredient.getUsedQuantity() + " " + historyIngredient.getUnit());


            PantryItem pantryItem = pantryItemRepository.findPantryItemByUserIdAndIngredientId(userId, historyIngredient.getIngredientId());

            double available = 0.0;

            if (pantryItem != null) {
                available = pantryItem.getQuantity();
            }


            if (available < historyIngredient.getUsedQuantity()) {

                missing.add(new MissingIngredientDTO(historyIngredient.getIngredientName(), historyIngredient.getUsedQuantity(), available, historyIngredient.getUsedQuantity() - available));
            }
        }

        return new CookingDTO(history.getRecipeName(), history.getDescription(), history.getInstructions(), history.getCategory(), ingredients, missing);
    }

    @Transactional
    public int repeatPreviousCook(Integer userId, Integer historyId) {

        User user = userRepository.findUserById(userId);

        // User not found
        if (user == null) {
            return 0;
        }

        CookingHistory history = cookingHistoryRepository.findCookingHistoryByIdAndUserId(historyId, userId);

        // History not found
        if (history == null ) {
            return 6;
        }

        List<CookingHisIng> historyIngredients = cookingHisIngRepository.findCookingHisIngByCookingHistoryId(historyId);
        if (historyIngredients.isEmpty()) return 6;


        // Check all ingredients first
        for (int i = 0; i < historyIngredients.size(); i++) {

            int result = haveTheIngredient(userId, historyIngredients.get(i).getIngredientId(), historyIngredients.get(i).getUsedQuantity());

            if (result == 3) {
                return 3;
            }

            if (result == 4) {
                return 4;
            }
        }


        // Remove ingredients
        for (int i = 0; i < historyIngredients.size(); i++) {
            removeIngredient(userId, historyIngredients.get(i).getIngredientId(), historyIngredients.get(i).getUsedQuantity());
        }


        // Create new history
        CookingHistory newHistory = new CookingHistory();

        newHistory.setUserId(userId);
        newHistory.setRecipeId(history.getRecipeId());
        newHistory.setRecipeName(history.getRecipeName());
        newHistory.setDescription(history.getDescription());
        newHistory.setInstructions(history.getInstructions());
        newHistory.setCategory(history.getCategory());
        newHistory.setRecipeType(history.getRecipeType());
        newHistory.setCookedAt(LocalDateTime.now());

        cookingHistoryRepository.save(newHistory);


        // Copy ingredient snapshot to new history
        for (int i = 0; i < historyIngredients.size(); i++) {

            CookingHisIng oldIngredient = historyIngredients.get(i);

            CookingHisIng newIngredient = new CookingHisIng();

            newIngredient.setCookingHistoryId(newHistory.getId());
            newIngredient.setIngredientId(oldIngredient.getIngredientId());
            newIngredient.setIngredientName(oldIngredient.getIngredientName());
            newIngredient.setUsedQuantity(oldIngredient.getUsedQuantity());
            newIngredient.setUnit(oldIngredient.getUnit());

            cookingHisIngRepository.save(newIngredient);
        }

        return 1;
    }

    @Transactional
    public int convertSystemRecipeToUserRecipe(Integer userId, Integer systemRecipeId) {

        User oldUser = userRepository.findUserById(userId);

        // user not found
        if (oldUser == null) {
            return 0;
        }

        SystemRecipe systemRecipe = systemRecipeRepository.findSystemRecipeById(systemRecipeId);

        // system recipe not found
        if (systemRecipe == null) {
            return 6;
        }

        UserRecipe checkUserRecipe = userRecipeRepository.findUserRecipeByUserIdAndName(userId, systemRecipe.getName());

        // recipe name already exists for this user
        if (checkUserRecipe != null) {
            return 3;
        }

        UserRecipe userRecipe = new UserRecipe();

        userRecipe.setUserId(userId);
        userRecipe.setName(systemRecipe.getName());
        userRecipe.setDescription(systemRecipe.getDescription());
        userRecipe.setInstructions(systemRecipe.getInstructions());
        userRecipe.setCategory(systemRecipe.getCategory());
        userRecipe.setImageUrl(systemRecipe.getImageUrl());

        userRecipeRepository.save(userRecipe);


        List<SystemRecIng> systemRecIngs = systemRecIngRepository.findSystemRecIngBySystemRecipeId(systemRecipeId);

        for (int i = 0; i < systemRecIngs.size(); i++) {

            UserRecIng userRecIng = new UserRecIng();

            userRecIng.setUserRecipeId(userRecipe.getId());
            userRecIng.setIngredientId(systemRecIngs.get(i).getIngredientId());
            userRecIng.setRequiredQuantity(systemRecIngs.get(i).getRequiredQuantity());

            userRecIngRepository.save(userRecIng);
        }

        return 1;
    }

}
