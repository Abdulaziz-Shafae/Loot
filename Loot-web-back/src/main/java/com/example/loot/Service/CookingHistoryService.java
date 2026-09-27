package com.example.loot.Service;

import com.example.loot.Model.CookingHistory;
import com.example.loot.Model.User;
import com.example.loot.Repository.CookingHistoryRepository;
import com.example.loot.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
//0 cooking history not found
//1 success
//2 user not found

public class CookingHistoryService {
    private final com.example.loot.Repository.CookingHisIngRepository children;
    private final com.example.loot.Security.CurrentUser currentUser;

    private final CookingHistoryRepository cookingHistoryRepository;
    private final UserRepository userRepository;


    public List<CookingHistory> getCookingHistory(){
        return cookingHistoryRepository.findAll().stream().filter(x -> x.getUserId().equals(currentUser.id())).toList();
    }


    public int addCookingHistory(CookingHistory cookingHistory){
        cookingHistory.setId(null);
        cookingHistory.setUserId(currentUser.id());

        User checkUser = userRepository.findUserById(cookingHistory.getUserId());

        //user not found
        if(checkUser == null){
            return 2;
        }

        cookingHistory.setCookedAt(LocalDateTime.now());

        cookingHistoryRepository.save(cookingHistory);
        //added
        return 1;
    }


    public int editCookingHistory(Integer id, CookingHistory cookingHistory){

        CookingHistory oldCookingHistory = cookingHistoryRepository.findCookingHistoryById(id);
        if (oldCookingHistory != null) currentUser.owns(oldCookingHistory.getUserId());

        //cooking history not found
        if(oldCookingHistory == null){
            return 0;
        }

        oldCookingHistory.setRecipeName(cookingHistory.getRecipeName());
        oldCookingHistory.setDescription(cookingHistory.getDescription());
        oldCookingHistory.setInstructions(cookingHistory.getInstructions());
        oldCookingHistory.setCategory(cookingHistory.getCategory());
        oldCookingHistory.setRecipeType(cookingHistory.getRecipeType());

        cookingHistoryRepository.save(oldCookingHistory);
        //updated
        return 1;
    }


    @org.springframework.transaction.annotation.Transactional
    public int deleteCookingHistory(int id){

        CookingHistory oldCookingHistory = cookingHistoryRepository.findCookingHistoryById(id);
        if (oldCookingHistory != null) currentUser.owns(oldCookingHistory.getUserId());

        //cooking history not found
        if(oldCookingHistory == null){
            return 0;
        }

        children.deleteAll(children.findCookingHisIngByCookingHistoryId(id));
        cookingHistoryRepository.delete(oldCookingHistory);
        //deleted
        return 1;
    }
}