package com.example.Recipe_Sharing_WebApplication.Repository;

import com.example.Recipe_Sharing_WebApplication.Entity.Favorite;
import com.example.Recipe_Sharing_WebApplication.Entity.Recipe;
import com.example.Recipe_Sharing_WebApplication.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserAndRecipe(User user, Recipe recipe);

    Optional<Favorite> findByUserAndRecipe(User user, Recipe recipe);

    void deleteByUserAndRecipe(User user, Recipe recipe);

    Page<Favorite> findByUser(User user, Pageable pageable);

    List<Favorite> findByUser(User user);

    List<Favorite> findByRecipe(Recipe recipe);
}
