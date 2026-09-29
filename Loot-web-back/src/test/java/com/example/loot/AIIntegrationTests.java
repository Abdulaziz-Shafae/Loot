package com.example.loot;
import com.example.loot.Model.*;
import com.example.loot.DTO.*;
import com.example.loot.Repository.*;
import com.example.loot.Service.*;
import com.example.loot.Security.RateLimits;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AIIntegrationTests {
    @Autowired AIService ai;
    @Autowired UserRepository users;
    @Autowired IngredientRepository ingredients;
    @Autowired PantryItemRepository pantry;
    @Autowired SystemRecipeRepository recipes;
    @Autowired SystemRecIngRepository links;
    @Autowired UserRecipeRepository userRecipes;
    @Autowired ObjectMapper json;
    @MockitoBean EmailService mail;

    @Test void fiveToolsValidateAndSaveUsingActualServices() throws Exception {
        var u=new User();u.setName("AI Test");u.setEmail("ai@example.test");u.setPhoneNumber("0500000000");u.setPassword("unused-test-hash");u=users.save(u);
        var i=new Ingredient();i.setName("Oats");i.setUnit("g");i=ingredients.save(i);
        var p=new PantryItem();p.setUserId(u.getId());p.setIngredientId(i.getId());p.setQuantity(500.0);p.setLowStockThreshold(50.0);pantry.save(p);
        var alternative=new Ingredient();alternative.setName("Rice");alternative.setUnit("g");alternative=ingredients.save(alternative);
        var alternativeStock=new PantryItem();alternativeStock.setUserId(u.getId());alternativeStock.setIngredientId(alternative.getId());alternativeStock.setQuantity(500.0);alternativeStock.setLowStockThreshold(0.0);pantry.save(alternativeStock);
        var r=new SystemRecipe();r.setName("Oat Bowl");r.setCategory("Breakfast");r.setInstructions("Simmer oats");r.setDescription("Warm oats");r=recipes.save(r);
        var ri=new SystemRecIng();ri.setSystemRecipeId(r.getId());ri.setIngredientId(i.getId());ri.setRequiredQuantity(100.0);links.save(ri);
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        Object original=ReflectionTestUtils.getField(ai,"restClient");ReflectionTestUtils.setField(ai,"restClient",builder.build());
        try {
            String rec=r.getId()+"|Oat Bowl|System|Warm oats|Breakfast|Simmer oats|Uses your oats|NONE";
            for(String output:List.of("oats|100|g","Rice|50|g|A pantry option","Oats|50|g|Same ingredient",rec,rec,"RECIPE|Warm Oats|Simple breakfast|Breakfast|Simmer oats\nINGREDIENT|"+i.getId()+"|Oats|100|g")) {
                String response=json.writeValueAsString(Map.of("output",List.of(Map.of("type","message","content",List.of(Map.of("type","output_text","text",output))))));
                server.expect(requestTo("https://api.openai.com/v1/responses")).andRespond(withSuccess(response,MediaType.APPLICATION_JSON));
            }
            byte[] png=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jZ1kAAAAASUVORK5CYII=");
            var detected=ai.imageToIngredient(u.getId(),new MockMultipartFile("image","oats.png","image/png",png));assertEquals("Oats",detected.getName());assertEquals(3,ai.addImageIngredient(u.getId(),detected));
            assertEquals("Rice",ai.ingredientSubstitute(u.getId(),r.getId(),"system",i.getId()).getSubstitute());
            assertEquals("no substitute",ai.ingredientSubstitute(u.getId(),r.getId(),"system",i.getId()).getSubstitute());
            assertEquals(1,ai.recipeRecommendation(u.getId(),"Breakfast please").size());
            assertEquals(1,ai.leftoverRescue(u.getId(),List.of(new LeftoverDTO("Oats",20.0,"g"))).size());
            var generated=ai.recipeGenerator(u.getId(),"Warm breakfast");assertNotNull(generated);assertEquals(1,ai.addGeneratedRecipe(u.getId(),generated));assertEquals(1,userRecipes.findUserRecipeByUserId(u.getId()).size());server.verify();
        } finally { ReflectionTestUtils.setField(ai,"restClient",original); }
    }
    @Test void limiterRejectsBurstWithoutGrowingIndefinitely() { var limits=new RateLimits();assertTrue(limits.allow("test",2,60));assertTrue(limits.allow("test",2,60));assertFalse(limits.allow("test",2,60));assertTrue(limits.allow("other",2,60)); }
}
