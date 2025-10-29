package com.example.Recipe_Sharing_WebApplication.Controller;

import com.example.Recipe_Sharing_WebApplication.DTO.RecipeDTO;
import com.example.Recipe_Sharing_WebApplication.Entity.Recipe;
import com.example.Recipe_Sharing_WebApplication.Service.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing recipes (CRUD + search + category list).
 */
@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
@Tag(name = "Recipes", description = "Endpoints for managing and browsing recipes")
public class RecipeController {

    private final RecipeService recipeService;

    /**
     * Get all recipes with optional filtering and pagination.
     * Example: GET /api/recipes?page=0&size=8&title=pasta&category=DESSERT
     */
    @Operation(summary = "List recipes", description = "Retrieve all recipes, optionally filtered by title or category.")
    @GetMapping
    public Page<RecipeDTO> getAllRecipes(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "category", required = false) String category
    ) {
        if (title != null || category != null) {
            return recipeService.search(title, category, PageRequest.of(page, size));
        }
        return recipeService.findAll(PageRequest.of(page, size));
    }

    /**
     * Get a specific recipe by ID.
     */
    @Operation(summary = "Get recipe by ID", description = "Retrieve a specific recipe using its unique ID.")
    @GetMapping("/{id}")
    public ResponseEntity<RecipeDTO> getRecipeById(@PathVariable Long id) {
        return recipeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new recipe (requires authentication).
     */
    @Operation(summary = "Create recipe", description = "Authenticated users can create a new recipe.")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecipeDTO> createRecipe(@RequestBody @Valid Recipe recipe) {
        return ResponseEntity.status(201).body(recipeService.create(recipe));
    }

    /**
     * Update an existing recipe (only owner or admin can update).
     */
    @Operation(summary = "Update recipe", description = "Recipe owner or admin can update recipe details.")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @recipeService.isOwner(#id)")
    public ResponseEntity<?> updateRecipe(@PathVariable Long id, @RequestBody @Valid Recipe recipe) {
        return recipeService.update(id, recipe)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a recipe (only owner or admin can delete).
     */
    @Operation(summary = "Delete recipe", description = "Recipe owner or admin can delete the recipe.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @recipeService.isOwner(#id)")
    public ResponseEntity<?> deleteRecipe(@PathVariable Long id) {
        return recipeService.delete(id)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    /**
     * Get all available recipe categories.
     */
    @Operation(summary = "List categories", description = "Returns all available recipe categories.")
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(recipeService.getAllCategories());
    }
}
