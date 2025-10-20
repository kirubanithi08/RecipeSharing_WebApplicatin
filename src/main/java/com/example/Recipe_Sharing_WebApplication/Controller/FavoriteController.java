package com.example.Recipe_Sharing_WebApplication.Controller;

import com.example.Recipe_Sharing_WebApplication.DTO.FavoriteRecipeDTO;
import com.example.Recipe_Sharing_WebApplication.Service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{recipeId}")
    public ResponseEntity<String> addFavorite(@PathVariable Long recipeId) {
        favoriteService.addFavorite(recipeId);
        return ResponseEntity.status(201).body("Recipe added to favorites!");
    }

    @DeleteMapping("/{recipeId}")
    public ResponseEntity<String> removeFavorite(@PathVariable Long recipeId) {
        favoriteService.removeFavorite(recipeId);
        return ResponseEntity.ok("Recipe removed from favorites!");
    }

    @GetMapping
    public ResponseEntity<List<FavoriteRecipeDTO>> getUserFavorites() {
        return ResponseEntity.ok(favoriteService.getUserFavorites());
    }
}
