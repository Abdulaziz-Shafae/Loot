# Original endpoint inventory

| Method | Path | Controller signature (parameters/body) |
|---|---|---|
| POST | `/api/v1/ai/image/to/ingredient` | `imageToIngredient(@RequestParam(value = "image", required = false) MultipartFile image, HttpSession session)` |
| POST | `/api/v1/ai/image/to/ingredient/add` | `addImageIngredient(@RequestBody ImageToIngredientDTO imageToIngredientDTO, HttpSession session)` |
| POST | `/api/v1/ai/ingredient/substitute/{recipeId}/{listType}/{ingredientId}` | `ingredientSubstitute(@PathVariable Integer recipeId, @PathVariable String listType, @PathVariable Integer ingredientId, HttpSession session)` |
| POST | `/api/v1/ai/recipe/recommendation` | `recipeRecommendation(@RequestBody @Valid RecipeGeneratorRequestDTO requestDTO, Errors errors, HttpSession session)` |
| POST | `/api/v1/ai/leftover/rescue` | `leftoverRescue(@RequestBody @Valid List<LeftoverDTO> leftovers, Errors errors, HttpSession session)` |
| POST | `/api/v1/ai/recipe/generator` | `recipeGenerator(@RequestBody @Valid RecipeGeneratorRequestDTO requestDTO, Errors errors, HttpSession session)` |
| POST | `/api/v1/ai/recipe/generator/add` | `addGeneratedRecipe(@RequestBody GeneratedRecipeDTO recipeDTO, HttpSession session)` |
| GET | `/api/v1/history/ingredient/get` | `getCookingHisIng()` |
| POST | `/api/v1/history/ingredient/add` | `addCookingHisIng(@RequestBody @Valid CookingHisIng cookingHisIng, Errors errors)` |
| PUT | `/api/v1/history/ingredient/update/{id}` | `editCookingHisIng(@PathVariable Integer id, @RequestBody @Valid CookingHisIng cookingHisIng, Errors errors)` |
| DELETE | `/api/v1/history/ingredient/delete/{id}` | `deleteCookingHisIng(@PathVariable Integer id)` |
| GET | `/api/v1/history/get` | `getCookingHistory()` |
| POST | `/api/v1/history/add` | `addCookingHistory(@RequestBody @Valid CookingHistory cookingHistory, Errors errors)` |
| PUT | `/api/v1/history/update/{id}` | `editCookingHistory(@PathVariable Integer id, @RequestBody @Valid CookingHistory cookingHistory, Errors errors)` |
| DELETE | `/api/v1/history/delete/{id}` | `deleteCookingHistory(@PathVariable Integer id)` |
| GET | `/api/v1/ingredient/get` | `getIngredients()` |
| POST | `/api/v1/ingredient/add` | `addIngredient(@RequestBody @Valid Ingredient ingredient, Errors errors)` |
| PUT | `/api/v1/ingredient/update/{id}` | `editIngredient(@PathVariable Integer id, @RequestBody @Valid Ingredient ingredient, Errors errors)` |
| DELETE | `/api/v1/ingredient/delete/{id}` | `deleteIngredient(@PathVariable Integer id)` |
| GET | `/api/v1/pantry/get` | `getPantryItems()` |
| POST | `/api/v1/pantry/add` | `addPantryItem(@RequestBody @Valid PantryItem pantryItem, Errors errors)` |
| PUT | `/api/v1/pantry/update/{id}` | `editPantryItem(@PathVariable Integer id, @RequestBody @Valid PantryItem pantryItem, Errors errors)` |
| DELETE | `/api/v1/pantry/delete/{id}` | `deletePantryItem(@PathVariable Integer id)` |
| GET | `/api/v1/system/ingredient/get` | `getSystemRecIngs()` |
| POST | `/api/v1/system/ingredient/add` | `addSystemRecIng(@RequestBody @Valid SystemRecIng systemRecIng, Errors errors)` |
| PUT | `/api/v1/system/ingredient/update/{id}` | `editSystemRecIng(@PathVariable Integer id, @RequestBody @Valid SystemRecIng systemRecIng, Errors errors)` |
| DELETE | `/api/v1/system/ingredient/delete/{id}` | `deleteSystemRecIng(@PathVariable Integer id)` |
| GET | `/api/v1/system/get` | `getSystemRecipes()` |
| POST | `/api/v1/system/add` | `addSystemRecipe(@RequestBody @Valid SystemRecipe systemRecipe, Errors errors)` |
| PUT | `/api/v1/system/update/{id}` | `editSystemRecipe(@PathVariable Integer id, @RequestBody @Valid SystemRecipe systemRecipe, Errors errors)` |
| DELETE | `/api/v1/system/delete/{id}` | `deleteSystemRecipe(@PathVariable Integer id)` |
| GET | `/api/v1/user/get` | `getUsers()` |
| POST | `/api/v1/user/add` | `addUser(@RequestBody @Valid User user, Errors errors)` |
| PUT | `/api/v1/user/update/{id}` | `editUser(@PathVariable Integer id, @RequestBody @Valid User user, Errors errors)` |
| DELETE | `/api/v1/user/delete/{id}` | `deleteUser(@PathVariable Integer id)` |
| POST | `/api/v1/user/login` | `login(@RequestBody @Valid LoginDTO loginDTO, Errors errors, HttpSession session)` |
| POST | `/api/v1/user/logout` | `logout(HttpSession session)` |
| POST | `/api/v1/user/forgotPassword/{email}` | `forgotPassword(@PathVariable String email)` |
| POST | `/api/v1/user/forgotPassword/{email}/{code}` | `forgotPasswordCode(@PathVariable String email, @PathVariable Integer code, @RequestBody @Valid LoginDTO loginDTO, Errors errors)` |
| GET | `/api/v1/user/can/{listType}/{recipeId}` | `canCookRecipe(@PathVariable String listType, @PathVariable Integer recipeId, HttpSession session)` |
| GET | `/api/v1/user/missing/{listType}/{recipeId}` | `missingList(@PathVariable String listType, @PathVariable Integer recipeId, HttpSession session)` |
| GET | `/api/v1/user/system` | `getSystemRecipes()` |
| GET | `/api/v1/user/system/{category}` | `getSystemRecipesByCategory(@PathVariable String category)` |
| GET | `/api/v1/user/possible/system/{category}` | `systemRecipesByCategory(@PathVariable String category, HttpSession session)` |
| GET | `/api/v1/user/almost/system/{category}` | `systemRecipesByCategoryAlmost(@PathVariable String category, HttpSession session)` |
| GET | `/api/v1/user/user` | `getUserRecipes(HttpSession session)` |
| GET | `/api/v1/user/user/{category}` | `getUserRecipesByCategory(@PathVariable String category, HttpSession session)` |
| GET | `/api/v1/user/possible/user/{category}` | `userRecipesByCategory(@PathVariable String category, HttpSession session)` |
| GET | `/api/v1/user/almost/user/{category}` | `userRecipesByCategoryAlmost(@PathVariable String category, HttpSession session)` |
| GET | `/api/v1/user/low` | `lowStock(HttpSession session)` |
| POST | `/api/v1/user/low/email` | `lowStockEmail(HttpSession session)` |
| GET | `/api/v1/user/cook/{listType}/{recipeId}` | `cookRecipe(@PathVariable String listType, @PathVariable Integer recipeId, HttpSession session)` |
| POST | `/api/v1/user/cook/{listType}/{recipeId}/done` | `cookRecipeDone(@PathVariable String listType, @PathVariable Integer recipeId, HttpSession session)` |
| GET | `/api/v1/user/history` | `getHistoryByUserId(HttpSession session)` |
| GET | `/api/v1/user/history/{category}` | `getHistoryByCategory(@PathVariable String category, HttpSession session)` |
| GET | `/api/v1/user/possible/history/{category}` | `possibleHistoryByCategory(@PathVariable String category, HttpSession session)` |
| GET | `/api/v1/user/almost/history/{category}` | `almostHistoryByCategory(@PathVariable String category, HttpSession session)` |
| GET | `/api/v1/user/history/{historyId}/repeat` | `getPreviousCook(@PathVariable Integer historyId, HttpSession session)` |
| POST | `/api/v1/user/history/{historyId}/repeat/done` | `repeatPreviousCook(@PathVariable Integer historyId, HttpSession session)` |
| POST | `/api/v1/user/system/{recipeId}/convert` | `convertSystemRecipeToUserRecipe(@PathVariable Integer recipeId, HttpSession session)` |
| GET | `/api/v1/recipe/ingredient/get` | `getUserRecIng()` |
| POST | `/api/v1/recipe/ingredient/add` | `addUserRecIng(@RequestBody @Valid UserRecIng userRecIng, Errors errors)` |
| PUT | `/api/v1/recipe/ingredient/update/{id}` | `editUserRecIng(@PathVariable Integer id, @RequestBody @Valid UserRecIng userRecIng, Errors errors)` |
| DELETE | `/api/v1/recipe/ingredient/delete/{id}` | `deleteUserRecIng(@PathVariable Integer id)` |
| GET | `/api/v1/recipe/get` | `getUserRecipes()` |
| POST | `/api/v1/recipe/add` | `addUserRecipe(@RequestBody @Valid UserRecipe userRecipe, Errors errors)` |
| PUT | `/api/v1/recipe/update/{id}` | `editUserRecipe(@PathVariable Integer id, @RequestBody @Valid UserRecipe userRecipe, Errors errors)` |
| DELETE | `/api/v1/recipe/delete/{id}` | `deleteUserRecipe(@PathVariable Integer id)` |

Responses: GET CRUD returns entity lists; mutations return ApiResponse(message) or validation string. User matching returns RecipesDTO/AlmostRecipeDTO, cooking previews CookingDTO, missing MissingIngredientDTO, low-stock LowStockDTO. AI responses use ImageToIngredientDTO, IngredientSubstituteDTO, AIRecipeDTO lists, or GeneratedRecipeDTO. Empty matching results may be ApiResponse rather than arrays. Original authentication: CRUD public; personal user/AI functions read HttpSession.userId without an authentication filter.
