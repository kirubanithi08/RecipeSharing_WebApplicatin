
package com.example.Recipe_Sharing_WebApplication.Repository;

import com.example.Recipe_Sharing_WebApplication.Entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface  RecipeRepository extends JpaRepository<Recipe, Long> {

    @Query("SELECT r FROM Recipe r JOIN FETCH r.author")
    Page<Recipe> findAllWithAuthor(Pageable pageable);

    @Query("SELECT r FROM Recipe r WHERE " +
            "(:title IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:category IS NULL OR LOWER(r.category) = LOWER(:category))")
    Page<Recipe> search(String title, String category, Pageable pageable);
}
