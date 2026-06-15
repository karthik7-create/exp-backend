package com.expensewise.controller;

import com.expensewise.dto.CategoryDTO;
import com.expensewise.dto.CategoryRequest;
import com.expensewise.entity.User;
import com.expensewise.service.AuthService;
import com.expensewise.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final AuthService authService;

    public CategoryController(CategoryService categoryService, AuthService authService) {
        this.categoryService = categoryService;
        this.authService = authService;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = authService.getCurrentUser(auth.getName());
        return user.getId();
    }

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getCategories(@RequestParam(required = false) String type) {
        Long userId = getCurrentUserId();
        List<CategoryDTO> categories;
        if (type != null && !type.isBlank()) {
            categories = categoryService.getCategoriesByType(userId, type.toUpperCase());
        } else {
            categories = categoryService.getCategories(userId);
        }
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryRequest request) {
        Long userId = getCurrentUserId();
        CategoryDTO category = categoryService.createCategory(userId, request);
        return ResponseEntity.ok(category);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDTO> updateCategory(@PathVariable Long id,
                                                       @Valid @RequestBody CategoryRequest request) {
        Long userId = getCurrentUserId();
        CategoryDTO category = categoryService.updateCategory(userId, id, request);
        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        categoryService.deleteCategory(userId, id);
        return ResponseEntity.noContent().build();
    }
}
