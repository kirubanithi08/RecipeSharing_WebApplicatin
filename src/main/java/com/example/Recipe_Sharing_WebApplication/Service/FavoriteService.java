package com.example.Recipe_Sharing_WebApplication.Service;

import com.example.Recipe_Sharing_WebApplication.DTO.FavoriteRecipeDTO;
import com.example.Recipe_Sharing_WebApplication.Entity.Favorite;
import com.example.Recipe_Sharing_WebApplication.Entity.Recipe;
import com.example.Recipe_Sharing_WebApplication.Entity.User;
import com.example.Recipe_Sharing_WebApplication.Repository.FavoriteRepository;
import com.example.Recipe_Sharing_WebApplication.Repository.RecipeRepository;
import com.example.Recipe_Sharing_WebApplication.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void addFavorite(Long recipeId) {
        User user = getCurrentUser();
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        if (favoriteRepository.existsByUserAndRecipe(user, recipe)) {
            throw new RuntimeException("Recipe already favorited");
        }

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setRecipe(recipe);
        favoriteRepository.save(favorite);
    }

    public void removeFavorite(Long recipeId) {
        User user = getCurrentUser();
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        favoriteRepository.findByUserAndRecipe(user, recipe)
                .ifPresent(favoriteRepository::delete);
    }

    public List<FavoriteRecipeDTO> getUserFavorites() {
        User user = getCurrentUser();
        return favoriteRepository.findByUser(user).stream()
                .map(fav -> new FavoriteRecipeDTO(
                        fav.getRecipe().getId(),
                        fav.getRecipe().getTitle(),
                        fav.getRecipe().getCategory().name(),
                        fav.getRecipe().getAuthor().getUsername()))
                .toList();
    }
}
