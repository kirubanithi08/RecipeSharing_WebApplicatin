package com.example.Recipe_Sharing_WebApplication.Controller;

import com.example.Recipe_Sharing_WebApplication.DTO.RecipeDTO;
import com.example.Recipe_Sharing_WebApplication.Entity.Recipe;
import com.example.Recipe_Sharing_WebApplication.Service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping
    public Page<RecipeDTO> all(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size) {
        return recipeService.findAll(PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeDTO> get(@PathVariable Long id) {
        return recipeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()") // any logged-in user can create
    public ResponseEntity<RecipeDTO> create(@RequestBody @Valid Recipe recipe) {
        return ResponseEntity.status(201).body(recipeService.create(recipe));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @recipeService.isOwner(#id)")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody @Valid Recipe recipe) {
        return recipeService.update(id, recipe)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @recipeService.isOwner(#id)")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return recipeService.delete(id) ?
                ResponseEntity.ok().build() :
                ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public Page<RecipeDTO> search(@RequestParam(required = false) String title,
                                  @RequestParam(required = false) String category,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        return recipeService.search(title, category, PageRequest.of(page, size));
    }
}
