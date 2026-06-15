package com.expensewise.service;

import com.expensewise.dto.CategoryDTO;
import com.expensewise.dto.CategoryRequest;
import com.expensewise.entity.Category;
import com.expensewise.exception.BadRequestException;
import com.expensewise.exception.ResourceNotFoundException;
import com.expensewise.repository.BudgetRepository;
import com.expensewise.repository.CategoryRepository;
import com.expensewise.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           TransactionRepository transactionRepository,
                           BudgetRepository budgetRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
    }

    public List<CategoryDTO> getCategories(Long userId) {
        return categoryRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<CategoryDTO> getCategoriesByType(Long userId, String type) {
        return categoryRepository.findByUserIdAndType(userId, type).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public CategoryDTO createCategory(Long userId, CategoryRequest request) {
        Category category = Category.builder()
                .userId(userId)
                .name(request.getName())
                .type(request.getType().toUpperCase())
                .icon(request.getIcon())
                .color(request.getColor())
                .isDefault(false)
                .build();

        category = categoryRepository.save(category);
        return toDTO(category);
    }

    public CategoryDTO updateCategory(Long userId, Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        if (!category.getUserId().equals(userId)) {
            throw new BadRequestException("You do not have permission to update this category");
        }

        category.setName(request.getName());
        category.setType(request.getType().toUpperCase());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());

        category = categoryRepository.save(category);
        return toDTO(category);
    }

    public void deleteCategory(Long userId, Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        if (!category.getUserId().equals(userId)) {
            throw new BadRequestException("You do not have permission to delete this category");
        }

        if (category.isDefault()) {
            throw new BadRequestException("Cannot delete a default category");
        }

        if (transactionRepository.existsByCategoryId(id) || budgetRepository.existsByCategoryId(id)) {
            throw new BadRequestException("Cannot delete category because it is used by existing transactions or budgets. Please reassign or delete them first.");
        }

        categoryRepository.delete(category);
    }

    private CategoryDTO toDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .icon(category.getIcon())
                .color(category.getColor())
                .isDefault(category.isDefault())
                .build();
    }
}
