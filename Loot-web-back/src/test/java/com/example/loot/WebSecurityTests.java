package com.example.loot;

import com.example.loot.Model.*;
import com.example.loot.Repository.*;
import com.example.loot.Service.EmailService;
import com.example.loot.Security.ImageValidation;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
@AutoConfigureMockMvc
class WebSecurityTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    @Autowired UserRepository users;
    @Autowired PantryItemRepository pantry;
    @Autowired IngredientRepository ingredients;
    @Autowired UserRecipeRepository recipes;
    @Autowired UserRecIngRepository recipeIngredients;
    @Autowired SystemRecipeRepository systems;
    @Autowired SystemRecIngRepository systemIngredients;
    @Autowired CookingHistoryRepository history;
    @Autowired CookingHisIngRepository historyIngredients;
    @MockitoBean EmailService email;
    @Autowired com.example.loot.Service.AccountService accounts;
    @Autowired com.example.loot.Security.RateLimits limits;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    org.springframework.security.web.context.HttpSessionSecurityContextRepository contexts;
    User alice,bob,admin;
    final String password="Kitchen12!";

    @BeforeEach void setup() {
        // Tests share localhost and a Spring context; keep real limits isolated per test.
        ((Map<?,?>)org.springframework.test.util.ReflectionTestUtils.getField(limits,"buckets")).clear();
        historyIngredients.deleteAll();history.deleteAll();recipeIngredients.deleteAll();recipes.deleteAll();systemIngredients.deleteAll();systems.deleteAll();pantry.deleteAll();ingredients.deleteAll();users.deleteAll();
        alice=user("Alice","USER");bob=user("Bob","USER");admin=user("Admin","ADMIN");
    }
    User user(String name,String role){var u=new User();u.setName(name);u.setEmail(name.toLowerCase()+UUID.randomUUID()+"@example.test");u.setPhoneNumber("0500000000");u.setPassword(encoder.encode(password));u.setRole(role);return users.save(u);}
    MockHttpSession login(User u) throws Exception {
        var result=mvc.perform(post("/api/v1/user/login").with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("email",u.getEmail(),"password",password)))).andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.resetTokenHash").doesNotExist()).andReturn();
        return (MockHttpSession)result.getRequest().getSession(false);
    }
    Ingredient ingredient(){var i=new Ingredient();i.setName("Rice");i.setUnit("g");return ingredients.save(i);}
    PantryItem stock(User u,Ingredient i,double quantity){var p=new PantryItem();p.setUserId(u.getId());p.setIngredientId(i.getId());p.setQuantity(quantity);p.setLowStockThreshold(50.0);return pantry.save(p);}
    SystemRecipe system(Ingredient i){var r=new SystemRecipe();r.setName("Rice Bowl");r.setDescription("");r.setInstructions("Cook rice until tender.");r.setCategory("Dinner");r.setImageUrl("https://example.test/rice.png");r=systems.save(r);var link=new SystemRecIng();link.setSystemRecipeId(r.getId());link.setIngredientId(i.getId());link.setRequiredQuantity(100.0);systemIngredients.save(link);return r;}

    @Test void anonymousCsrfAndRoleBoundaries() throws Exception {
        // Only the exact POST reset endpoints are exempt, including for authenticated callers.
        mvc.perform(post("/api/v1/user/add").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/user/reset-password").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/user/reset-password/extra").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/pantry/get")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/user/login").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        var session=login(alice);
        mvc.perform(put("/api/v1/user/me/password").session(session).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/user/low/email").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/user/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/overview").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/system/add").session(session).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/user/get").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/overview").session(login(admin))).andExpect(status().isOk());
        mvc.perform(options("/api/v1/user/me").header("Origin","https://evil.example").header("Access-Control-Request-Method","GET")).andExpect(status().isForbidden());
        mvc.perform(options("/api/v1/user/me").header("Origin","http://localhost:5173").header("Access-Control-Request-Method","GET")).andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5173"));
    }
    @Test void realCsrfTokenSessionRotationAndLogout() throws Exception {
        var tokenResponse=mvc.perform(get("/api/v1/user/csrf")).andExpect(status().isOk()).andReturn();
        var session=(MockHttpSession)tokenResponse.getRequest().getSession(false);String oldId=session.getId();
        var token=json.readTree(tokenResponse.getResponse().getContentAsString());
        mvc.perform(post("/api/v1/user/login").session(session).header(token.get("headerName").asText(),token.get("token").asText()).contentType("application/json").content(json.writeValueAsString(Map.of("email",alice.getEmail(),"password",password)))).andExpect(status().isOk());
        assertNotEquals(oldId,session.getId());
        mvc.perform(get("/api/v1/user/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(alice.getEmail()));
        mvc.perform(post("/api/v1/user/logout").session(session).with(csrf())).andExpect(status().isOk());
        assertTrue(session.isInvalid());
    }
    @Test void registrationCannotChooseRoleOrIdAndHashesPassword() throws Exception {
        String address="new"+UUID.randomUUID()+"@example.test";
        mvc.perform(post("/api/v1/user/add").with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("id",alice.getId(),"name","New User","email",address,"password",password,"phoneNumber","0500000000","role","ADMIN")))).andExpect(status().isOk());
        var u=users.findUserByEmail(address);assertEquals("USER",u.getRole());assertNotEquals(alice.getId(),u.getId());assertNotEquals(password,u.getPassword());assertTrue(encoder.matches(password,u.getPassword()));
        verify(email).sendWelcomeEmail(address,"New User");
    }
    @Test void signupAuthenticatesRotatesSessionAndRenewsCsrf() throws Exception {
        var tokenResponse=mvc.perform(get("/api/v1/user/csrf")).andReturn();
        var session=(MockHttpSession)tokenResponse.getRequest().getSession(false);
        var oldId=session.getId();
        var token=json.readTree(tokenResponse.getResponse().getContentAsString());
        String address="signup"+UUID.randomUUID()+"@example.test";
        mvc.perform(post("/api/v1/user/add").session(session).header(token.get("headerName").asText(),token.get("token").asText()).contentType("application/json")
            .content(json.writeValueAsString(Map.of("name","New Cook","email",address,"password",password,"phoneNumber","0500000000"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.authenticated").value(true));
        assertNotEquals(oldId,session.getId());
        mvc.perform(get("/api/v1/user/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(address)).andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(post("/api/v1/user/logout").session(session).header(token.get("headerName").asText(),token.get("token").asText())).andExpect(status().isForbidden());
        var fresh=json.readTree(mvc.perform(get("/api/v1/user/csrf").session(session)).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/api/v1/user/logout").session(session).header(fresh.get("headerName").asText(),fresh.get("token").asText())).andExpect(status().isOk());
        assertTrue(session.isInvalid());
    }
    @Test void signupSessionFailureKeepsAccountAndFallsBackSafely() throws Exception {
        doThrow(new IllegalStateException("Test session failure")).when(contexts).saveContext(any(),any(),any());
        String address="fallback"+UUID.randomUUID()+"@example.test";
        var response=mvc.perform(post("/api/v1/user/add").with(csrf()).contentType("application/json")
            .content(json.writeValueAsString(Map.of("name","New Cook","email",address,"password",password,"phoneNumber","0500000000"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.authenticated").value(false)).andReturn();
        assertNotNull(users.findUserByEmail(address));
        assertNull(response.getRequest().getSession(false));
    }
    @Test void numericAndAiSaveValidationRejectsInvalidRequests() throws Exception {
        var i=ingredient();var session=login(alice);
        for(double quantity:List.of(-0.5,100000001.0)) {
            mvc.perform(post("/api/v1/pantry/add").session(session).with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("ingredientId",i.getId(),"quantity",quantity,"lowStockThreshold",0))))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/v1/ai/recipe/generator/add").session(session).with(csrf()).contentType("application/json")
            .content("{\"name\":\"Rice\",\"category\":\"Dinner\",\"instructions\":\"Cook\",\"ingredients\":[null]}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/ai/image/to/ingredient/add").session(session).with(csrf()).contentType("application/json")
            .content("{\"name\":\"Rice\",\"quantity\":-1,\"unit\":\"g\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test void rescueRejectsForeignStockDuplicatesWrongUnitsAndExcess() throws Exception {
        var i=ingredient();stock(bob,i,300);var session=login(alice);
        String valid="[{\"name\":\"Rice\",\"quantity\":20,\"unit\":\"g\"}]";
        mvc.perform(post("/api/v1/ai/leftover/rescue").session(session).with(csrf()).contentType("application/json").content(valid)).andExpect(status().isBadRequest());
        stock(alice,i,100);
        for(String body:List.of(valid.replace("20","101"),valid.replace("g\"","ml\""),valid.replace("]",",{\"name\":\"rice\",\"quantity\":1,\"unit\":\"g\"}]"),valid.replace("20","-1")))
            mvc.perform(post("/api/v1/ai/leftover/rescue").session(session).with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        assertEquals(100,pantry.findPantryItemByUserIdAndIngredientId(alice.getId(),i.getId()).getQuantity());
    }
    @Test void pantryOwnershipCrudAndMassAssignment() throws Exception {
        var i=ingredient();var other=stock(bob,i,300);var session=login(alice);
        mvc.perform(get("/api/v1/pantry/get").session(session)).andExpect(status().isOk()).andExpect(content().json("[]"));
        String body=json.writeValueAsString(Map.of("id",other.getId(),"userId",bob.getId(),"ingredientId",i.getId(),"quantity",200,"lowStockThreshold",20));
        mvc.perform(post("/api/v1/pantry/add").session(session).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
        var own=pantry.findPantryItemByUserIdAndIngredientId(alice.getId(),i.getId());assertNotNull(own);assertEquals(300,pantry.findById(other.getId()).orElseThrow().getQuantity());
        mvc.perform(put("/api/v1/pantry/update/"+other.getId()).session(session).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/pantry/delete/"+other.getId()).session(session).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/pantry/update/"+own.getId()).session(session).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/pantry/delete/"+own.getId()).session(session).with(csrf())).andExpect(status().isOk());
    }
    @Test void userRecipeCrudAndNestedOwnership() throws Exception {
        var i=ingredient();var session=login(alice);var otherSession=login(bob);
        String body=json.writeValueAsString(Map.of("userId",bob.getId(),"name","My Rice","description","Lunch","category","Lunch","instructions","Cook carefully"));
        var response=mvc.perform(post("/api/v1/recipe/add").session(session).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andReturn();
        int id=json.readTree(response.getResponse().getContentAsString()).get("id").asInt();assertEquals(alice.getId(),recipes.findById(id).orElseThrow().getUserId());
        String ing=json.writeValueAsString(Map.of("userRecipeId",id,"ingredientId",i.getId(),"requiredQuantity",100));
        mvc.perform(post("/api/v1/recipe/ingredient/add").session(otherSession).with(csrf()).contentType("application/json").content(ing)).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/recipe/ingredient/add").session(session).with(csrf()).contentType("application/json").content(ing)).andExpect(status().isOk());
        int link=recipeIngredients.findUserRecIngByUserRecipeId(id).getFirst().getId();
        mvc.perform(delete("/api/v1/recipe/ingredient/delete/"+link).session(otherSession).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/user/cook/user/"+id).session(otherSession)).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/recipe/update/"+id).session(otherSession).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/recipe/update/"+id).session(session).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/recipe/delete/"+id).session(session).with(csrf())).andExpect(status().isOk());assertTrue(recipeIngredients.findUserRecIngByUserRecipeId(id).isEmpty());
    }
    @Test void cookConvertRepeatAndMissingQuantities() throws Exception {
        var i=ingredient();var r=system(i);var p=stock(alice,i,250);var session=login(alice);
        mvc.perform(get("/api/v1/user/availability/system").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$[0].canCook").value(true));
        mvc.perform(post("/api/v1/user/system/"+r.getId()+"/convert").session(session).with(csrf())).andExpect(status().isOk());
        assertEquals(r.getImageUrl(),recipes.findUserRecipeByUserId(alice.getId()).getFirst().getImageUrl());
        mvc.perform(post("/api/v1/user/cook/system/"+r.getId()+"/done").session(session).with(csrf())).andExpect(status().isOk());
        assertEquals(150,pantry.findById(p.getId()).orElseThrow().getQuantity());var h=history.findCookingHistoryByUserId(alice.getId()).getFirst();assertEquals(1,historyIngredients.findCookingHisIngByCookingHistoryId(h.getId()).size());
        mvc.perform(get("/api/v1/user/history/"+h.getId()+"/repeat").session(login(bob))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/user/history/"+h.getId()+"/repeat/done").session(session).with(csrf())).andExpect(status().isOk());
        assertEquals(50,pantry.findById(p.getId()).orElseThrow().getQuantity());assertEquals(2,history.findCookingHistoryByUserId(alice.getId()).size());
        mvc.perform(get("/api/v1/user/history/"+h.getId()+"/repeat").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.missing[0].missing").value(50));
        mvc.perform(post("/api/v1/user/cook/system/"+r.getId()+"/done").session(session).with(csrf())).andExpect(status().isBadRequest());
        assertEquals(50,pantry.findById(p.getId()).orElseThrow().getQuantity());
        mvc.perform(get("/api/v1/user/almost/system/Dinner").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$[0].missing[0].missing").value(50));
        mvc.perform(get("/api/v1/user/low").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Rice"));
    }
    @Test void emptyRecipeCannotCreateFreeCookingHistory() throws Exception {
        var r=new SystemRecipe();r.setName("Empty Recipe");r.setCategory("Dinner");r.setInstructions("Add ingredients first");r=systems.save(r);
        var session=login(alice);
        mvc.perform(get("/api/v1/user/availability/system").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$[0].canCook").value(false));
        mvc.perform(post("/api/v1/user/cook/system/"+r.getId()+"/done").session(session).with(csrf())).andExpect(status().isBadRequest());
        assertTrue(history.findCookingHistoryByUserId(alice.getId()).isEmpty());
    }
    @Test void imageSpoofAndPrivateAiRecipeAreRejected() throws Exception {
        var session=login(alice);
        mvc.perform(multipart("/api/v1/ai/image/to/ingredient").file(new MockMultipartFile("image","fake.png","image/png","not an image".getBytes())).session(session).with(csrf())).andExpect(status().isBadRequest());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->ImageValidation.validate(new MockMultipartFile("image","huge.jpg","image/jpeg",new byte[6*1024*1024])));
        var r=new UserRecipe();r.setName("Private Rice");r.setInstructions("Secret recipe");r.setCategory("Dinner");r.setUserId(bob.getId());recipes.save(r);var i=ingredient();
        mvc.perform(post("/api/v1/ai/ingredient/substitute/"+r.getId()+"/user/"+i.getId()).session(session).with(csrf())).andExpect(status().isBadRequest());
    }
    @Test void authenticatedPasswordChangeWorksWithoutEmail() throws Exception {
        var session=login(alice);
        mvc.perform(put("/api/v1/user/me/password").session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("currentPassword",password,"password","NewKitchen12!")))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/user/login").with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("email",alice.getEmail(),"password",password)))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/user/login").with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("email",alice.getEmail(),"password","NewKitchen12!")))).andExpect(status().isOk());
        verifyNoInteractions(email);
    }
    @Test void lowStockEmailReportsEmptySuccessAndProviderFailure() throws Exception {
        var session=login(alice);
        mvc.perform(post("/api/v1/user/low/email").session(session).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("No low stock ingredients"));
        verifyNoInteractions(email);
        stock(alice,ingredient(),1);
        mvc.perform(post("/api/v1/user/low/email").session(session).with(csrf())).andExpect(status().isOk());
        verify(email).sendLowStockEmail(eq(alice.getEmail()),eq(alice.getName()),anyList());
        doThrow(new com.example.loot.Service.EmailDeliveryException("Unavailable")).when(email).sendLowStockEmail(anyString(),anyString(),anyList());
        mvc.perform(post("/api/v1/user/low/email").session(session).with(csrf())).andExpect(status().isServiceUnavailable());
    }
    String resetToken() throws Exception {
        mvc.perform(post("/api/v1/user/forgot-password").contentType("application/json").content(json.writeValueAsString(Map.of("email",alice.getEmail())))).andExpect(status().isOk());
        var link=org.mockito.ArgumentCaptor.forClass(String.class);
        verify(email,atLeastOnce()).sendPasswordResetLink(eq(alice.getEmail()),eq(alice.getName()),link.capture());
        return link.getValue().substring(link.getValue().indexOf("?token=")+7);
    }
    String resetBody(String token) throws Exception { return json.writeValueAsString(Map.of("token",token,"newPassword","NewKitchen12!")); }
    @Test void resetLinkIsGenericHashedAndReplacesPreviousToken(org.springframework.boot.test.system.CapturedOutput output) throws Exception {
        String token=resetToken();
        var stored=users.findById(alice.getId()).orElseThrow();
        assertEquals(43,token.length());
        assertEquals(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8))),stored.getResetTokenHash());
        assertFalse(stored.toString().contains(token));
        assertTrue(stored.getResetExpiresAt().isAfter(Instant.now().plusSeconds(86300)));
        assertTrue(stored.getResetExpiresAt().isBefore(Instant.now().plusSeconds(86401)));
        assertFalse(new com.example.loot.DTO.AuthRequests.Reset(token,"NewKitchen12!").toString().contains(token));
        String replacement=resetToken();assertNotEquals(token,replacement);
        mvc.perform(post("/api/v1/user/reset-password").contentType("application/json").content(resetBody(token))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/user/forgot-password").contentType("application/json").content("{\"email\":\"unknown@example.test\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("If an account exists for this email, a password reset link has been sent. Email delivery may take some time."));
        verify(email,times(2)).sendPasswordResetLink(anyString(),anyString(),anyString());
        assertNull(users.findById(bob.getId()).orElseThrow().getResetTokenHash());
        assertFalse(output.getAll().contains(token));
        assertFalse(output.getAll().contains(replacement));
    }
    @Test void resetLinkExpiresIsSingleUseChangesPasswordAndRevokesSessions() throws Exception {
        var session=login(alice);
        String token=resetToken();
        mvc.perform(post("/api/v1/user/reset-password").contentType("application/json").content(resetBody("x".repeat(43)))).andExpect(status().isBadRequest());
        var stored=users.findById(alice.getId()).orElseThrow();stored.setResetExpiresAt(Instant.now().minusSeconds(1));users.save(stored);
        mvc.perform(post("/api/v1/user/reset-password").contentType("application/json").content(resetBody(token))).andExpect(status().isBadRequest());
        token=resetToken();
        mvc.perform(post("/api/v1/user/reset-password").contentType("application/json").content(resetBody(token))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/user/reset-password").contentType("application/json").content(resetBody(token))).andExpect(status().isBadRequest());
        stored=users.findById(alice.getId()).orElseThrow();assertNull(stored.getResetTokenHash());assertNull(stored.getResetExpiresAt());
        mvc.perform(get("/api/v1/user/me").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/user/login").with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("email",alice.getEmail(),"password",password)))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/user/login").with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("email",alice.getEmail(),"password","NewKitchen12!")))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/user/reset-password").contentType("application/json").content(json.writeValueAsString(Map.of("email",alice.getEmail(),"code","123456","password",password)))).andExpect(status().isBadRequest());
    }
    @Test void resetEmailFailureClearsTokenWithoutRevealingAccount() throws Exception {
        doThrow(new com.example.loot.Service.EmailDeliveryException("Provider unavailable")).when(email).sendPasswordResetLink(anyString(),anyString(),anyString());
        mvc.perform(post("/api/v1/user/forgot-password").contentType("application/json").content(json.writeValueAsString(Map.of("email",alice.getEmail())))).andExpect(status().isOk());
        var stored=users.findById(alice.getId()).orElseThrow();assertNull(stored.getResetTokenHash());assertNull(stored.getResetExpiresAt());
    }
}
