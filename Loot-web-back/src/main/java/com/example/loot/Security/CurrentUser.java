package com.example.loot.Security;

import com.example.loot.Repository.*;
import com.example.loot.Model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
@RequiredArgsConstructor
public class CurrentUser {
    private final UserRepository users;
    private final UserRecipeRepository recipes;
    private final CookingHistoryRepository histories;
    public Integer id() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in");
        return Integer.valueOf(auth.getName());
    }
    public User user() { return users.findById(id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)); }
    public void owns(Integer owner) { if (!id().equals(owner)) throw new AccessDeniedException("Access denied"); }
    public UserRecipe requireUserRecipe(Integer id) {
        var r = recipes.findUserRecipeByIdAndUserId(id, id());
        if (r == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found");
        return r;
    }
    public CookingHistory requireCookingHistory(Integer id) {
        var h = histories.findCookingHistoryByIdAndUserId(id, id());
        if (h == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "History not found");
        return h;
    }
    public boolean ownsUserRecipeQuietly(Integer id) { return recipes.findUserRecipeByIdAndUserId(id, id()) != null; }
    public boolean ownsCookingHistoryQuietly(Integer id) { return histories.findCookingHistoryByIdAndUserId(id, id()) != null; }
}
