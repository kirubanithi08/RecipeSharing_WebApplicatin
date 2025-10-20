package com.example.Recipe_Sharing_WebApplication.DTO;

public record FavoriteRecipeDTO(
        Long id,
        String title,
        String category,
        String authorUsername
) {}
