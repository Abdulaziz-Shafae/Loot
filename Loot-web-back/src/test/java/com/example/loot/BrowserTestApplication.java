package com.example.loot;

import com.example.loot.Model.*;
import com.example.loot.Repository.*;
import com.example.loot.Service.EmailService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Explicit, test-classpath-only browser fixture. Never packaged in the production JAR. */
public class BrowserTestApplication {
    public static void main(String[] args) {
        var localArgs = new java.util.ArrayList<>(java.util.List.of(args));
        localArgs.add("--server.address=127.0.0.1");
        SpringApplication.from(LootApplication::main).with(Fixtures.class).run(localArgs.toArray(String[]::new));
    }
    @TestConfiguration
    static class Fixtures {
        @Bean @Primary EmailService localOnlyMail() { return org.mockito.Mockito.mock(EmailService.class); }
        @Bean CommandLineRunner browserFixtures(UserRepository users, IngredientRepository ingredients, SystemRecipeRepository recipes, SystemRecIngRepository links,PasswordEncoder encoder) {
            return args -> {
                var user=new User();user.setName("Loot Tester");user.setEmail("admin@example.test");user.setPassword(encoder.encode("Kitchen12!"));user.setPhoneNumber("0500000000");user.setRole("ADMIN");users.save(user);
                var rice=new Ingredient();rice.setName("Rice");rice.setUnit("g");rice=ingredients.save(rice);
                var egg=new Ingredient();egg.setName("Egg");egg.setUnit("piece");ingredients.save(egg);
                var recipe=new SystemRecipe();recipe.setName("Simple Rice Bowl");recipe.setCategory("Dinner");recipe.setDescription("A comforting bowl for a quiet evening.");recipe.setInstructions("1. Rinse the rice.\n2. Simmer in water until tender.\n3. Serve warm.");recipe=recipes.save(recipe);
                var link=new SystemRecIng();link.setSystemRecipeId(recipe.getId());link.setIngredientId(rice.getId());link.setRequiredQuantity(100.0);links.save(link);
            };
        }
    }
}
