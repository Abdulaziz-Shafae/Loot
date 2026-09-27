package com.example.loot.Controller;
import com.example.loot.Repository.*;
import com.example.loot.Service.UserService;
import com.example.loot.Security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class AvailabilityController {
    private final UserService service;
    private final SystemRecipeRepository systems;
    private final UserRecipeRepository recipes;
    private final CurrentUser current;
    @GetMapping("/api/v1/user/availability/{type}")
    public List<Map<String,Object>> availability(@PathVariable String type) {
        var user=current.id();
        List<Integer> ids;
        if(type.equals("system"))ids=systems.findAll().stream().map(r->r.getId()).toList();
        else if(type.equals("user"))ids=recipes.findUserRecipeByUserId(user).stream().map(r->r.getId()).toList();
        else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid recipe type");
        return ids.stream().map(id->{ var missing=service.missingList(user,id,type); return Map.<String,Object>of("id",id,"canCook",service.canCookRecipe(user,id,type)==1,"almost",missing.size()>=1 && missing.size()<=3,"missing",missing); }).toList();
    }
}
