package com.example.Recipe_Sharing_WebApplication.DTO;


import java.time.LocalDateTime;

public record RecipeDTO(
        Long id,
        String title,
        String description,
        String category,
        String instructions,
        String authorUsername,
        LocalDateTime createdAt
) {}


