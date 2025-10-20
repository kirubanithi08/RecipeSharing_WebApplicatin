package com.example.Recipe_Sharing_WebApplication.Service;

import com.example.Recipe_Sharing_WebApplication.DTO.RecipeDTO;
import com.example.Recipe_Sharing_WebApplication.Entity.Recipe;
import com.example.Recipe_Sharing_WebApplication.Entity.User;
import com.example.Recipe_Sharing_WebApplication.Repository.RecipeRepository;
import com.example.Recipe_Sharing_WebApplication.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;


    public Page<RecipeDTO> findAll(Pageable pageable) {
        return recipeRepository.findAllWithAuthor(pageable).map(this::mapToDTO);
    }

    public Optional<RecipeDTO> findById(Long id) {
        return recipeRepository.findById(id).map(this::mapToDTO);
    }


    public RecipeDTO create(Recipe recipe) {
        User author = getCurrentUserFromJWT();
        recipe.setAuthor(author);
        return mapToDTO(recipeRepository.save(recipe));
    }


    public Optional<RecipeDTO> update(Long id, Recipe updates) {
        return recipeRepository.findById(id).map(existing -> {
            if (updates.getTitle() != null) existing.setTitle(updates.getTitle());
            if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
            if (updates.getCategory() != null) existing.setCategory(updates.getCategory());
            return mapToDTO(recipeRepository.save(existing));
        });
    }


    public boolean delete(Long id) {
        return recipeRepository.findById(id).map(recipe -> {
            recipeRepository.delete(recipe);
            return true;
        }).orElse(false);
    }

    public Page<RecipeDTO> search(String title, String category, Pageable pageable) {
        return recipeRepository.search(title, category, pageable).map(this::mapToDTO);
    }


    public boolean isOwner(Long recipeId) {
        User currentUser = getCurrentUserFromJWT();
        return recipeRepository.findById(recipeId)
                .map(recipe -> recipe.getAuthor().getId().equals(currentUser.getId()))
                .orElse(false);
    }


    private RecipeDTO mapToDTO(Recipe recipe) {
        return new RecipeDTO(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getCategory().name(),
                recipe.getAuthor().getUsername(),
                recipe.getCreatedAt()
        );
    }


    private User getCurrentUserFromJWT() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
