package com.example.loot.Service;

import com.example.loot.DTO.AIRecipeDTO;
import com.example.loot.DTO.GeneratedRecipeDTO;
import com.example.loot.DTO.GeneratedRecipeIngredientDTO;
import com.example.loot.DTO.ImageToIngredientDTO;
import com.example.loot.DTO.IngredientSubstituteDTO;
import com.example.loot.DTO.LeftoverDTO;
import com.example.loot.Model.*;
import com.example.loot.Repository.*;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class AIService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model:gpt-5.6-luna}")
    private String model;
    private final RestClient restClient = RestClient.builder().requestFactory(requestFactory()).build();
    private static org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); factory.setReadTimeout(60000); return factory;
    }
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;
    private final PantryItemRepository pantryItemRepository;
    private final SystemRecipeRepository systemRecipeRepository;
    private final SystemRecIngRepository systemRecIngRepository;
    private final UserRecipeRepository userRecipeRepository;
    private final UserRecIngRepository userRecIngRepository;
    private final UserService userService;


    public ImageToIngredientDTO imageToIngredient(Integer userId, MultipartFile image) {

        User user = userRepository.findUserById(userId);

        // User not found
        if (user == null) {
            return null;
        }

        // No image uploaded
        if (image == null || image.isEmpty()) {
            return new ImageToIngredientDTO("no img", 0.0, "");
        }

        com.example.loot.Security.ImageValidation.validate(image);

        // Make sure it is an image
        String contentType = image.getContentType();

        if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/webp"))) {
            return new ImageToIngredientDTO("wrong img", 0.0, "");
        }

        try {

            // Convert image to Base64
            String base64Image = Base64.getEncoder().encodeToString(image.getBytes());

            String imageUrl = "data:" + contentType + ";base64," + base64Image;


            String prompt = """
            Identify the main food ingredient in this image.
        
            Return ONLY this format:
            name|quantity|unit
        
            Rules:
            - name must be the ingredient name in English.
            - quantity must always be a number.
            - unit must always be exactly one of: g, ml, piece.
            - Never return an empty unit.
        
            - Use piece only when the ingredient can clearly and naturally be counted
              as individual items, such as eggs, tomatoes, apples, onions, whole chicken, etc.
        
            - Use g for solid ingredients that are normally measured by weight.
            - Use ml for liquids.
        
            - NEVER guess or estimate grams or milliliters based only on the visual size
              of the ingredient, bowl, plate, cup, container, or portion.
        
            - Only return a non-zero quantity for g or ml when there is reliable visible
              evidence of the exact amount, such as:
                * a readable package label,
                * a visible kitchen scale,
                * or another explicit measurement shown in the image.
        
            - If the ingredient uses g or ml but the exact quantity cannot be reliably
              determined from the image, return 0 for quantity.
        
            - For piece, return the number of clearly visible countable items.
        
            - Do not assume standard serving sizes.
            - Do not invent measurements.
        
            - If the ingredient can be identified, always choose the most appropriate unit
              from g, ml, or piece even if the quantity is 0.
        
            - If you cannot identify the ingredient at all, return:
              unknown|0|piece
        
            Examples:
            rice|0|g
            milk|0|ml
            chicken breast|0|g
            apple|1|piece
            eggs|3|piece
            whole chicken|1|piece
            """;


            Map<String, Object> textContent = Map.of("type", "input_text", "text", prompt);

            Map<String, Object> imageContent = Map.of("type", "input_image", "image_url", imageUrl);

            Map<String, Object> input = Map.of("role", "user", "content", List.of(textContent, imageContent));

            Map<String, Object> body = Map.of("model", model, "input", List.of(input));


            String response = restClient.post().uri("https://api.openai.com/v1/responses").header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);

            // Extract the AI text from the response
            String result = extractOutputText(response);

            if (result == null || result.isBlank()) {
                return new ImageToIngredientDTO("ai format error", 0.0, "piece");
            }

            // Example: Tomato|3|piece
            String[] parts = result.trim().split("\\|", -1);

            if (parts.length != 3) {
                return new ImageToIngredientDTO("ai format error", 0.0, "piece");
            }


            String name = parts[0].trim();
            String quantityString = parts[1].trim();
            String unit = parts[2].trim();

            if (!(unit.equals("g") || unit.equals("ml") || unit.equals("piece"))) {
                return new ImageToIngredientDTO("ai format error", 0.0, "piece");
            }

            if (name.isEmpty() || name.equalsIgnoreCase("unknown")) {
                return new ImageToIngredientDTO("not identified", 0.0, "piece");
            }

            Double quantity;

            try {
                quantity = Double.parseDouble(quantityString);
            } catch (NumberFormatException e) {
                quantity = 0.0;
            }


// Check if ingredient already exists in Loot
            String normalizedName = normalizeIngredientName(name);

            for (Ingredient ingredient : ingredientRepository.findAll()) {

                String currentName = normalizeIngredientName(ingredient.getName());

                if (currentName.equals(normalizedName)) {

                    // Use Loot database name
                    name = ingredient.getName();

                    // If AI unit is different from Loot unit,
                    // use Loot unit and reset quantity because we cannot convert safely
                    if (!ingredient.getUnit().equals(unit)) {
                        return new ImageToIngredientDTO(
                                ingredient.getName(),
                                0.0,
                                ingredient.getUnit()
                        );
                    }

                    break;
                }
            }


            return new ImageToIngredientDTO(name, quantity, unit);


        } catch (IOException e) {

            return new ImageToIngredientDTO("image error", 0.0, "");
        }
    }

    @Transactional
    public int addImageIngredient(Integer userId, ImageToIngredientDTO imageToIngredientDTO) {

        User user = userRepository.findUserById(userId);

        // User not found
        if (user == null) {
            return 0;
        }

        if (imageToIngredientDTO == null) {
            return 4;
        }

        // Get data from DTO
        String name = imageToIngredientDTO.getName();
        Double quantity = imageToIngredientDTO.getQuantity();
        String unit = imageToIngredientDTO.getUnit();


        // Invalid data
        if (name == null || name.trim().isEmpty() || quantity == null || !Double.isFinite(quantity) || quantity <= 0 || unit == null || !(unit.equals("g") || unit.equals("ml") || unit.equals("piece"))) {
            return 4;
        }


        name = name.trim();


        // Check if ingredient already exists
        String normalizedName = normalizeIngredientName(name);

        Ingredient ingredient = null;

        for (Ingredient currentIngredient : ingredientRepository.findAll()) {

            String currentName = normalizeIngredientName(currentIngredient.getName());

            if (currentName.equals(normalizedName)) {
                ingredient = currentIngredient;
                break;
            }
        }

        // Ingredient does not exist
        if (ingredient == null) {

            Ingredient newIngredient = new Ingredient();

            newIngredient.setName(name);
            newIngredient.setUnit(unit);

            ingredientRepository.save(newIngredient);


            // Add it to user's pantry
            PantryItem pantryItem = new PantryItem();

            pantryItem.setUserId(userId);
            pantryItem.setIngredientId(newIngredient.getId());
            pantryItem.setQuantity(quantity);
            pantryItem.setLowStockThreshold(0.0);

            pantryItemRepository.save(pantryItem);

            return 1;
        }


        // Existing ingredient must use the same unit
        if (!ingredient.getUnit().equals(unit)) {
            return 5;
        }


        // Check user's pantry
        PantryItem pantryItem = pantryItemRepository.findPantryItemByUserIdAndIngredientId(userId, ingredient.getId());


        // Ingredient exists but user doesn't have it
        if (pantryItem == null) {

            PantryItem newPantryItem = new PantryItem();

            newPantryItem.setUserId(userId);
            newPantryItem.setIngredientId(ingredient.getId());
            newPantryItem.setQuantity(quantity);
            newPantryItem.setLowStockThreshold(0.0);

            pantryItemRepository.save(newPantryItem);

            return 2;
        }


        // Ingredient already exists in user's pantry
        pantryItem.setQuantity(pantryItem.getQuantity() + quantity);

        pantryItemRepository.save(pantryItem);

        return 3;
    }

    public IngredientSubstituteDTO ingredientSubstitute(Integer userId, Integer recipeId, String listType, Integer ingredientId) {

        User user = userRepository.findUserById(userId);

        // User not found
        if (user == null) {
            return null;
        }


        Ingredient missingIngredient = ingredientRepository.findIngredientById(ingredientId);

        // Ingredient not found
        if (missingIngredient == null) {
            return new IngredientSubstituteDTO("ingredient not found", "", 0.0, "", "");
        }


        String recipeName;
        Double requiredQuantity;


        // System Recipe
        if (listType.equalsIgnoreCase("system")) {

            SystemRecipe recipe = systemRecipeRepository.findSystemRecipeById(recipeId);

            if (recipe == null) {
                return new IngredientSubstituteDTO("recipe not found", "", 0.0, "", "");
            }


            SystemRecIng recipeIngredient = systemRecIngRepository.findSystemRecIngBySystemRecipeIdAndIngredientId(recipeId, ingredientId);

            if (recipeIngredient == null) {
                return new IngredientSubstituteDTO("ingredient not in recipe", "", 0.0, "", "");
            }


            recipeName = recipe.getName();
            requiredQuantity = recipeIngredient.getRequiredQuantity();
        }


        // User Recipe
        else if (listType.equalsIgnoreCase("user")) {

            UserRecipe recipe = userRecipeRepository.findUserRecipeByIdAndUserId(recipeId, userId);

            if (recipe == null) {
                return new IngredientSubstituteDTO("recipe not found", "", 0.0, "", "");
            }


            UserRecIng recipeIngredient = userRecIngRepository.findUserRecIngByUserRecipeIdAndIngredientId(recipeId, ingredientId);

            if (recipeIngredient == null) {
                return new IngredientSubstituteDTO("ingredient not in recipe", "", 0.0, "", "");
            }


            recipeName = recipe.getName();
            requiredQuantity = recipeIngredient.getRequiredQuantity();
        }


        // Wrong list type
        else {
            return new IngredientSubstituteDTO("wrong list type", "", 0.0, "", "");
        }


        List<PantryItem> pantryItems = pantryItemRepository.findAll();

        StringBuilder pantry = new StringBuilder();


        for (PantryItem pantryItem : pantryItems) {

            if (!pantryItem.getUserId().equals(userId) || pantryItem.getQuantity() <= 0) {
                continue;
            }


            Ingredient ingredient = ingredientRepository.findIngredientById(pantryItem.getIngredientId());


            if (ingredient != null) {
                pantry.append("ID: ").append(ingredient.getId()).append(", Name: ").append(ingredient.getName()).append(", Available: ").append(pantryItem.getQuantity()).append(" ").append(ingredient.getUnit()).append("\n");
            }
        }


        // Pantry is empty
        if (pantry.isEmpty()) {
            return new IngredientSubstituteDTO(missingIngredient.getName(), "no substitute", 0.0, "", "");
        }


        String prompt = """
            Find a substitute for a missing ingredient in a recipe.

            Recipe:
            %s

            Missing ingredient:
            %s

            Required quantity:
            %s %s

            User pantry:
            %s

            Rules:
            - Recommend ONLY an ingredient from the user's pantry.
            - The substitute must make sense for this specific recipe.
            - Never recommend an ingredient outside the pantry.
            - Never invent an ingredient.
            - Return the exact ingredient name exactly as it appears in the user's pantry.
            - Never use more than the available pantry quantity.
            - If there is no suitable substitute, return exactly:
              NONE

            If there is a suitable substitute return ONLY:
            substitute|quantity|unit|reason

            - quantity must be a number.
            - unit must be g, ml, or piece.
            """.formatted(recipeName, missingIngredient.getName(), requiredQuantity, missingIngredient.getUnit(), pantry);


        String result = callOpenAI(prompt);


        // No substitute
        if (result == null || result.trim().equalsIgnoreCase("NONE")) {
            return new IngredientSubstituteDTO(missingIngredient.getName(), "no substitute", 0.0, "", "");
        }


        String[] parts = result.trim().split("\\|", -1);


        // Wrong AI format
        if (parts.length != 4) {
            return new IngredientSubstituteDTO(missingIngredient.getName(), "ai format error", 0.0, "", "");
        }


        String substitute = parts[0].trim();
        String quantityString = parts[1].trim();
        String unit = parts[2].trim();
        String reason = parts[3].trim();


        Double quantity;

        try {
            quantity = Double.parseDouble(quantityString);
        } catch (NumberFormatException e) {
            return new IngredientSubstituteDTO(missingIngredient.getName(), "ai format error", 0.0, "", "");
        }


        Ingredient substituteIngredient = ingredientRepository.findIngredientByNameIgnoreCase(substitute);


        // AI invented an ingredient
        if (substituteIngredient == null) {
            return new IngredientSubstituteDTO(missingIngredient.getName(), "no substitute", 0.0, "", "");
        }


        // Wrong unit returned by AI
        if (!substituteIngredient.getUnit().equals(unit)) {
            return new IngredientSubstituteDTO(missingIngredient.getName(), "no substitute", 0.0, "", "");
        }


        PantryItem pantryItem = pantryItemRepository.findPantryItemByUserIdAndIngredientId(userId, substituteIngredient.getId());


        // User does not actually have substitute
        if (pantryItem == null || pantryItem.getQuantity() <= 0 || !Double.isFinite(quantity) || quantity <= 0 || quantity > pantryItem.getQuantity()) {
            return new IngredientSubstituteDTO(missingIngredient.getName(), "no substitute", 0.0, "", "");
        }


        return new IngredientSubstituteDTO(missingIngredient.getName(), substituteIngredient.getName(), quantity, substituteIngredient.getUnit(), reason);
    }

    public List<AIRecipeDTO> recipeRecommendation(Integer userId, String request) {

        User user = userRepository.findUserById(userId);

        // User not found
        if (user == null) {
            return null;
        }


        StringBuilder pantry = buildPantryText(userId);

        StringBuilder recipes = new StringBuilder();


        // System Recipes
        List<SystemRecipe> systemRecipes = systemRecipeRepository.findAll();


        for (SystemRecipe recipe : systemRecipes) {

            int canCook = userService.canCookRecipe(userId, recipe.getId(), "system");


            // Only send recipes the user can cook
            if (canCook == 1) {
                recipes.append("TYPE=System").append("; ID=").append(recipe.getId()).append("; NAME=").append(recipe.getName()).append("; CATEGORY=").append(recipe.getCategory()).append("; DESCRIPTION=").append(recipe.getDescription()).append("; INSTRUCTIONS=").append(recipe.getInstructions()).append("\n");
            }
        }


        // User Recipes
        List<UserRecipe> userRecipes = userRecipeRepository.findUserRecipeByUserId(userId);


        for (UserRecipe recipe : userRecipes) {

            int canCook = userService.canCookRecipe(userId, recipe.getId(), "user");


            // Only send recipes the user can cook
            if (canCook == 1) {
                recipes.append("TYPE=User").append("; ID=").append(recipe.getId()).append("; NAME=").append(recipe.getName()).append("; CATEGORY=").append(recipe.getCategory()).append("; DESCRIPTION=").append(recipe.getDescription()).append("; INSTRUCTIONS=").append(recipe.getInstructions()).append("\n");
            }
        }


        String prompt = """
            Recommend recipes based on the user's request.
        
            User request:
            %s
        
            User pantry:
            %s
        
            Available recipes that Loot already verified the user can cook:
            %s
        
            Rules:
            - Return up to 3 recipes, preferably 3 when possible.
            - recipeType must be System, User, or Generated.
        
            - Prioritize recipes that best match the user's request.
        
            - For System and User recipes:
              * Use ONLY recipes from the provided available recipes list.
              * Preserve the exact recipeId.
              * Preserve the exact name.
              * Preserve the exact description.
              * Preserve the exact category.
              * Preserve the exact instructions.
              * ingredientsUsed must be NONE.
        
            - Generated recipes are allowed only when needed.
            - Generated recipes may use ONLY ingredients from the user's pantry.
            - Never invent ingredients.
            - Never use more than the available pantry quantity.
            - Generated recipes must use recipeId 0.
            - For Generated recipes, ingredientsUsed must contain every ingredient used.
        
            - category must be exactly one of:
              Breakfast, Lunch, Dinner, Snack.
        
            - ingredientsUsed format must be:
              ingredientName:quantity:unit,ingredientName:quantity:unit
        
            - Use the exact ingredient names and units from the pantry.
            - quantity must be greater than 0.
            - unit must be g, ml, or piece.
        
            - Do not use the | character inside any field.
        
            Return ONLY the recipe lines.
        
            Format:
            recipeId|name|recipeType|description|category|instruction|reason|ingredientsUsed
        
            Do not add headings, markdown, or explanations.
            """.formatted(request, pantry, recipes);

        String result = callOpenAI(prompt);


        return parseAIRecipes(userId, result, null);
    }

    public List<AIRecipeDTO> leftoverRescue(Integer userId, List<LeftoverDTO> leftovers) {

        User user = userRepository.findUserById(userId);

        // User not found
        if (user == null) {
            return null;
        }


        StringBuilder leftoverText = new StringBuilder();


        for (LeftoverDTO leftover : leftovers) {
            leftoverText.append(leftover.getName()).append(": ").append(leftover.getQuantity()).append(" ").append(leftover.getUnit()).append("\n");
        }


        StringBuilder pantry = buildPantryText(userId);

        StringBuilder recipes = new StringBuilder();


        // System Recipes
        List<SystemRecipe> systemRecipes = systemRecipeRepository.findAll();


        for (SystemRecipe recipe : systemRecipes) {

            boolean canCookNormally = userService.canCookRecipe(userId, recipe.getId(), "system") == 1;

            boolean canCookUsingLeftovers = canCookWithLeftovers(userId, recipe.getId(), "system", leftovers);


            if (canCookNormally || canCookUsingLeftovers) {
                recipes.append("TYPE=System").append("; ID=").append(recipe.getId()).append("; NAME=").append(recipe.getName()).append("; CATEGORY=").append(recipe.getCategory()).append("; DESCRIPTION=").append(recipe.getDescription()).append("; INSTRUCTIONS=").append(recipe.getInstructions()).append("\n");
            }
        }


        // User Recipes
        List<UserRecipe> userRecipes = userRecipeRepository.findUserRecipeByUserId(userId);


        for (UserRecipe recipe : userRecipes) {

            boolean canCookNormally = userService.canCookRecipe(userId, recipe.getId(), "user") == 1;

            boolean canCookUsingLeftovers = canCookWithLeftovers(userId, recipe.getId(), "user", leftovers);


            if (canCookNormally || canCookUsingLeftovers) {
                recipes.append("TYPE=User").append("; ID=").append(recipe.getId()).append("; NAME=").append(recipe.getName()).append("; CATEGORY=").append(recipe.getCategory()).append("; DESCRIPTION=").append(recipe.getDescription()).append("; INSTRUCTIONS=").append(recipe.getInstructions()).append("\n");
            }
        }


        String prompt = """
            The user wants to rescue leftovers.
        
            Leftovers:
            %s
        
            User pantry:
            %s
        
            Existing recipes Loot verified can be cooked using the pantry and/or supplied leftovers:
            %s
        
            Rules:
            - Return up to 3 recipes, preferably 3 when possible.
            - recipeType must be System, User, or Generated.
            - Prioritize recipes that use the supplied leftovers.
            - System and User recipes above are already verified by Loot.
            - Generated recipes may use ONLY the supplied leftovers and ingredients from the user's pantry.
            - Never invent ingredients.
            - Never use more than the available quantities.
            - category must be Breakfast, Lunch, Dinner, or Snack.
            - For System or User recipes preserve the exact ID, name, description, category, and instructions.
            - Generated recipes must use recipeId 0.
            - For Generated recipes, ingredientsUsed must contain every ingredient used.
            - ingredientsUsed format must be:
              ingredientName:quantity:unit,ingredientName:quantity:unit
            - Use the exact ingredient names and units from the pantry or leftovers.
            - For System and User recipes, ingredientsUsed must be NONE.
            - Do not use the | character inside any field.
        
            Return ONLY the recipe lines.
        
            Format:
            recipeId|name|recipeType|description|category|instruction|reason|ingredientsUsed
        
            Do not add headings or explanations.
            """.formatted(leftoverText, pantry, recipes);


        String result = callOpenAI(prompt);


        return parseAIRecipes(userId, result, leftovers);
    }

    public GeneratedRecipeDTO recipeGenerator(Integer userId, String request) {

        User user = userRepository.findUserById(userId);

        // User not found
        if (user == null) {
            return null;
        }


        List<PantryItem> pantryItems = pantryItemRepository.findAll();

        StringBuilder pantry = new StringBuilder();


        for (PantryItem pantryItem : pantryItems) {

            if (!pantryItem.getUserId().equals(userId) || pantryItem.getQuantity() <= 0) {
                continue;
            }


            Ingredient ingredient = ingredientRepository.findIngredientById(pantryItem.getIngredientId());


            if (ingredient != null) {
                pantry.append(ingredient.getId()).append("|").append(ingredient.getName()).append("|").append(pantryItem.getQuantity()).append("|").append(ingredient.getUnit()).append("\n");
            }
        }


        // Pantry is empty
        if (pantry.isEmpty()) {
            return null;
        }


        String prompt = """
            Generate one new recipe based on the user's request.

            User request:
            %s

            Pantry format:
            ingredientId|name|availableQuantity|unit

            Pantry:
            %s

            Rules:
            - Use ONLY ingredients from the pantry.
            - Never invent ingredients.
            - Never use more than the available quantity.
            - Use the exact ingredientId supplied.
            - category must be Breakfast, Lunch, Dinner, or Snack.
            - name must contain only English letters and spaces.
            - Keep description under 500 characters.
            - quantity must be greater than 0.

            Return ONLY:

            RECIPE|name|description|category|instructions
            INGREDIENT|ingredientId|name|quantity|unit
            INGREDIENT|ingredientId|name|quantity|unit

            Add one INGREDIENT line for every ingredient used.
            Do not add headings, markdown, or explanations.
            """.formatted(request, pantry);


        String result = callOpenAI(prompt);


        // No result
        if (result == null || result.isBlank()) {
            return null;
        }


        String[] lines = result.trim().split("\\R");


        // Recipe needs at least one ingredient
        if (lines.length < 2) {
            return null;
        }


        String[] recipeParts = lines[0].split("\\|", -1);


        // Wrong AI format
        if (recipeParts.length != 5 || !recipeParts[0].equals("RECIPE")) {
            return null;
        }


        String name = recipeParts[1].trim();
        String description = recipeParts[2].trim();
        String category = recipeParts[3].trim();
        String instructions = recipeParts[4].trim();


        // Invalid category
        if (!(category.equals("Breakfast") || category.equals("Lunch") || category.equals("Dinner") || category.equals("Snack"))) {
            return null;
        }


        List<GeneratedRecipeIngredientDTO> ingredients = new ArrayList<>();


        for (int i = 1; i < lines.length; i++) {

            String[] parts = lines[i].split("\\|", -1);


            if (parts.length != 5 || !parts[0].equals("INGREDIENT")) {
                continue;
            }


            try {

                Integer ingredientId = Integer.parseInt(parts[1].trim());

                Double quantity = Double.parseDouble(parts[3].trim());


                Ingredient ingredient = ingredientRepository.findIngredientById(ingredientId);

                PantryItem pantryItem = pantryItemRepository.findPantryItemByUserIdAndIngredientId(userId, ingredientId);


                // Verify AI output against database
                if (ingredient == null || pantryItem == null || !Double.isFinite(quantity) || quantity <= 0 || quantity > pantryItem.getQuantity()) {
                    continue;
                }


                // Avoid duplicate ingredient
                boolean duplicate = false;

                for (GeneratedRecipeIngredientDTO generatedIngredient : ingredients) {

                    if (generatedIngredient.getIngredientId().equals(ingredientId)) {
                        duplicate = true;
                        break;
                    }
                }


                if (duplicate) {
                    continue;
                }


                ingredients.add(new GeneratedRecipeIngredientDTO(ingredient.getId(), ingredient.getName(), quantity, ingredient.getUnit()));


            } catch (NumberFormatException e) {

                continue;
            }
        }


        // No valid ingredients
        if (ingredients.isEmpty()) {
            return null;
        }


        return new GeneratedRecipeDTO(name, description, category, instructions, ingredients);
    }

    @Transactional
    public int addGeneratedRecipe(Integer userId, GeneratedRecipeDTO recipeDTO) {

        User user = userRepository.findUserById(userId);

        // User not found
        if (user == null) {
            return 0;
        }


        // Invalid recipe data
        if (recipeDTO == null || recipeDTO.getName() == null || recipeDTO.getName().trim().isEmpty() || recipeDTO.getInstructions() == null || recipeDTO.getInstructions().trim().isEmpty() || recipeDTO.getCategory() == null || recipeDTO.getIngredients() == null || recipeDTO.getIngredients().isEmpty()) {
            return 3;
        }


        // Invalid recipe name
        if (!recipeDTO.getName().matches("^[A-Za-z ]+$")) {
            return 3;
        }


        // Invalid category
        if (!(recipeDTO.getCategory().equals("Breakfast") || recipeDTO.getCategory().equals("Lunch") || recipeDTO.getCategory().equals("Dinner") || recipeDTO.getCategory().equals("Snack"))) {
            return 3;
        }


        // Invalid name length
        if (recipeDTO.getName().length() > 150) {
            return 3;
        }


        // Invalid description length
        if (recipeDTO.getDescription() != null && recipeDTO.getDescription().length() > 500) {
            return 3;
        }


        // Check duplicate recipe name
        UserRecipe checkRecipe = userRecipeRepository.findUserRecipeByUserIdAndName(userId, recipeDTO.getName().trim());


        if (checkRecipe != null) {
            return 2;
        }


        List<Integer> ingredientIds = new ArrayList<>();


        // Validate all ingredients before saving
        for (GeneratedRecipeIngredientDTO dto : recipeDTO.getIngredients()) {

            if (dto.getIngredientId() == null || dto.getQuantity() == null || !Double.isFinite(dto.getQuantity()) || dto.getQuantity() <= 0) {
                return 4;
            }


            // Duplicate ingredient
            if (ingredientIds.contains(dto.getIngredientId())) {
                return 4;
            }


            ingredientIds.add(dto.getIngredientId());


            Ingredient ingredient = ingredientRepository.findIngredientById(dto.getIngredientId());

            PantryItem pantryItem = pantryItemRepository.findPantryItemByUserIdAndIngredientId(userId, dto.getIngredientId());


            if (ingredient == null || pantryItem == null || dto.getQuantity() > pantryItem.getQuantity()) {
                return 4;
            }
        }


        // Save recipe
        UserRecipe userRecipe = new UserRecipe();

        userRecipe.setUserId(userId);
        userRecipe.setName(recipeDTO.getName().trim());
        userRecipe.setDescription(recipeDTO.getDescription());
        userRecipe.setCategory(recipeDTO.getCategory());
        userRecipe.setInstructions(recipeDTO.getInstructions());

        userRecipeRepository.save(userRecipe);


        // Save recipe ingredients
        for (GeneratedRecipeIngredientDTO dto : recipeDTO.getIngredients()) {

            UserRecIng userRecIng = new UserRecIng();

            userRecIng.setUserRecipeId(userRecipe.getId());
            userRecIng.setIngredientId(dto.getIngredientId());
            userRecIng.setRequiredQuantity(dto.getQuantity());

            userRecIngRepository.save(userRecIng);
        }


        return 1;
    }

    private StringBuilder buildPantryText(Integer userId) {

        StringBuilder pantry = new StringBuilder();

        List<PantryItem> pantryItems = pantryItemRepository.findAll();


        for (PantryItem pantryItem : pantryItems) {

            if (!pantryItem.getUserId().equals(userId) || pantryItem.getQuantity() <= 0) {
                continue;
            }


            Ingredient ingredient = ingredientRepository.findIngredientById(pantryItem.getIngredientId());


            if (ingredient != null) {
                pantry.append("ID: ").append(ingredient.getId()).append(", Name: ").append(ingredient.getName()).append(", Quantity: ").append(pantryItem.getQuantity()).append(" ").append(ingredient.getUnit()).append("\n");
            }
        }


        return pantry;
    }

    private boolean canCookWithLeftovers(Integer userId, Integer recipeId, String listType, List<LeftoverDTO> leftovers) {

        Map<Integer, Double> available = new HashMap<>();


        // Add pantry quantities
        List<PantryItem> pantryItems = pantryItemRepository.findAll();


        for (PantryItem pantryItem : pantryItems) {

            if (pantryItem.getUserId().equals(userId)) {
                available.put(pantryItem.getIngredientId(), pantryItem.getQuantity());
            }
        }


        // Add leftovers
        for (LeftoverDTO leftover : leftovers) {

            Ingredient ingredient = ingredientRepository.findIngredientByNameIgnoreCase(leftover.getName());


            if (ingredient != null && ingredient.getUnit().equals(leftover.getUnit())) {
                available.put(ingredient.getId(), available.getOrDefault(ingredient.getId(), 0.0) + leftover.getQuantity());
            }
        }


        // System Recipe
        if (listType.equalsIgnoreCase("system")) {

            SystemRecipe recipe = systemRecipeRepository.findSystemRecipeById(recipeId);

            if (recipe == null) {
                return false;
            }


            List<SystemRecIng> recipeIngredients = systemRecIngRepository.findSystemRecIngBySystemRecipeId(recipeId);


            for (SystemRecIng recipeIngredient : recipeIngredients) {

                Double availableQuantity = available.getOrDefault(recipeIngredient.getIngredientId(), 0.0);


                if (availableQuantity < recipeIngredient.getRequiredQuantity()) {
                    return false;
                }
            }


            return true;
        }


        // User Recipe
        if (listType.equalsIgnoreCase("user")) {

            UserRecipe recipe = userRecipeRepository.findUserRecipeByIdAndUserId(recipeId, userId);

            if (recipe == null) {
                return false;
            }


            List<UserRecIng> recipeIngredients = userRecIngRepository.findUserRecIngByUserRecipeId(recipeId);


            for (UserRecIng recipeIngredient : recipeIngredients) {

                Double availableQuantity = available.getOrDefault(recipeIngredient.getIngredientId(), 0.0);


                if (availableQuantity < recipeIngredient.getRequiredQuantity()) {
                    return false;
                }
            }


            return true;
        }


        return false;
    }

    private List<AIRecipeDTO> parseAIRecipes(Integer userId, String result, List<LeftoverDTO> leftovers) {

        List<AIRecipeDTO> recipes = new ArrayList<>();


        if (result == null || result.isBlank()) {
            return recipes;
        }


        String[] lines = result.trim().split("\\R");


        for (String line : lines) {

            String[] parts = line.trim().split("\\|", -1);


            if (parts.length != 8) {
                continue;
            }


            Integer recipeId;


            try {
                recipeId = Integer.parseInt(parts[0].trim());
            } catch (NumberFormatException e) {
                continue;
            }


            String recipeType = parts[2].trim();


            if (!(recipeType.equals("System") || recipeType.equals("User") || recipeType.equals("Generated"))) {
                continue;
            }


            String category = parts[4].trim();


            if (!(category.equals("Breakfast") || category.equals("Lunch") || category.equals("Dinner") || category.equals("Snack"))) {
                continue;
            }


            String ingredientsUsed = parts[7].trim();


            // Generated Recipe
            if (recipeType.equals("Generated")) {

                recipeId = null;


                if (!validateGeneratedRecommendation(userId, ingredientsUsed, leftovers)) {
                    continue;
                }
            }


            // System and User recipes
            else {

                if (recipeId <= 0) {
                    continue;
                }


                // System Recipe
                if (recipeType.equals("System")) {

                    SystemRecipe systemRecipe = systemRecipeRepository.findSystemRecipeById(recipeId);

                    if (systemRecipe == null) {
                        continue;
                    }


                    // Recipe Recommendation
                    if (leftovers == null) {

                        if (userService.canCookRecipe(userId, recipeId, "system") != 1) {
                            continue;
                        }
                    }


                    // Leftover Rescue
                    else {

                        boolean canCookNormally = userService.canCookRecipe(userId, recipeId, "system") == 1;

                        boolean canCookUsingLeftovers = canCookWithLeftovers(userId, recipeId, "system", leftovers);


                        if (!canCookNormally && !canCookUsingLeftovers) {
                            continue;
                        }
                    }
                }


                // User Recipe
                if (recipeType.equals("User")) {

                    UserRecipe userRecipe = userRecipeRepository.findUserRecipeByIdAndUserId(recipeId, userId);

                    if (userRecipe == null) {
                        continue;
                    }


                    // Recipe Recommendation
                    if (leftovers == null) {

                        if (userService.canCookRecipe(userId, recipeId, "user") != 1) {
                            continue;
                        }
                    }


                    // Leftover Rescue
                    else {

                        boolean canCookNormally = userService.canCookRecipe(userId, recipeId, "user") == 1;

                        boolean canCookUsingLeftovers = canCookWithLeftovers(userId, recipeId, "user", leftovers);


                        if (!canCookNormally && !canCookUsingLeftovers) {
                            continue;
                        }
                    }
                }
            }


            recipes.add(new AIRecipeDTO(recipeId, parts[1].trim(), recipeType, parts[3].trim(), category, parts[5].trim(), parts[6].trim()));


            if (recipes.size() == 3) {
                break;
            }
        }


        return recipes;
    }

    private boolean validateGeneratedRecommendation(Integer userId, String ingredientsUsed, List<LeftoverDTO> leftovers) {

        if (ingredientsUsed == null || ingredientsUsed.isBlank() || ingredientsUsed.equalsIgnoreCase("NONE")) {
            return false;
        }


        Map<String, Double> availableQuantity = new HashMap<>();

        Map<String, String> availableUnit = new HashMap<>();


        // Add pantry
        List<PantryItem> pantryItems = pantryItemRepository.findAll();


        for (PantryItem pantryItem : pantryItems) {

            if (!pantryItem.getUserId().equals(userId) || pantryItem.getQuantity() <= 0) {
                continue;
            }


            Ingredient ingredient = ingredientRepository.findIngredientById(pantryItem.getIngredientId());


            if (ingredient != null) {

                String name = ingredient.getName().trim().toLowerCase();

                availableQuantity.put(name, pantryItem.getQuantity());

                availableUnit.put(name, ingredient.getUnit());
            }
        }


        // Add leftovers
        if (leftovers != null) {

            for (LeftoverDTO leftover : leftovers) {

                String name = leftover.getName().trim().toLowerCase();


                if (availableUnit.containsKey(name) && !availableUnit.get(name).equals(leftover.getUnit())) {
                    continue;
                }


                availableUnit.put(name, leftover.getUnit());

                availableQuantity.put(name, availableQuantity.getOrDefault(name, 0.0) + leftover.getQuantity());
            }
        }


        Map<String, Double> requestedQuantity = new HashMap<>();


        String[] ingredients = ingredientsUsed.split(",");


        for (String ingredientText : ingredients) {

            String[] parts = ingredientText.trim().split(":", -1);


            if (parts.length != 3) {
                return false;
            }


            String name = parts[0].trim().toLowerCase();

            String quantityString = parts[1].trim();

            String unit = parts[2].trim();


            if (name.isEmpty() || !(unit.equals("g") || unit.equals("ml") || unit.equals("piece"))) {
                return false;
            }


            Double quantity;


            try {
                quantity = Double.parseDouble(quantityString);
            } catch (NumberFormatException e) {
                return false;
            }


            if (!Double.isFinite(quantity) || quantity <= 0) {
                return false;
            }


            // Ingredient does not exist in pantry or leftovers
            if (!availableQuantity.containsKey(name)) {
                return false;
            }


            // Wrong unit
            if (!availableUnit.get(name).equals(unit)) {
                return false;
            }


            requestedQuantity.put(name, requestedQuantity.getOrDefault(name, 0.0) + quantity);
        }


        // Make sure AI did not use more than available
        for (String name : requestedQuantity.keySet()) {

            if (requestedQuantity.get(name) > availableQuantity.get(name)) {
                return false;
            }
        }


        return true;
    }

    private String extractOutputText(String response) {

        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode root = objectMapper.readTree(response);

        JsonNode output = root.path("output");


        for (JsonNode item : output) {

            if (!item.path("type").asText("").equals("message")) {
                continue;
            }


            JsonNode content = item.path("content");


            for (JsonNode contentItem : content) {

                if (contentItem.path("type").asText("").equals("output_text")) {
                    return contentItem.path("text").asText("");
                }
            }
        }


        return null;
    }

    private String normalizeIngredientName(String name) {

        name = name.trim().toLowerCase();

        // tomatoes -> tomato
        // potatoes -> potato
        if (name.endsWith("oes") && name.length() > 3) {
            return name.substring(0, name.length() - 2);
        }

        // berries -> berry
        if (name.endsWith("ies") && name.length() > 3) {
            return name.substring(0, name.length() - 3) + "y";
        }

        // eggs -> egg
        // apples -> apple
        if (name.endsWith("s") && !name.endsWith("ss") && !name.endsWith("us") && !name.endsWith("ous") && name.length() > 1) {
            return name.substring(0, name.length() - 1);
        }

        return name;
    }

    private String callOpenAI(String prompt) {

        Map<String, Object> body = Map.of("model", model, "input", prompt);

        String response = restClient.post().uri("https://api.openai.com/v1/responses").header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);

        return extractOutputText(response);
    }
}