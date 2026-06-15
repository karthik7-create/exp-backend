package com.expensewise.config;

import com.expensewise.entity.Category;
import com.expensewise.entity.User;
import com.expensewise.repository.CategoryRepository;
import com.expensewise.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public DataInitializer(UserRepository userRepository, CategoryRepository categoryRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            List<Category> existing = categoryRepository.findByUserId(user.getId());
            if (existing.isEmpty()) {
                createDefaultCategories(user.getId());
            }
        }
    }

    public void createDefaultCategories(Long userId) {
        // Default expense categories
        categoryRepository.save(Category.builder()
                .userId(userId).name("Food & Dining").type("EXPENSE").icon("🍔").color("#FF6B6B").isDefault(true).build());
        categoryRepository.save(Category.builder()
                .userId(userId).name("Transportation").type("EXPENSE").icon("🚗").color("#4ECDC4").isDefault(true).build());
        categoryRepository.save(Category.builder()
                .userId(userId).name("Shopping").type("EXPENSE").icon("🛍️").color("#45B7D1").isDefault(true).build());
        categoryRepository.save(Category.builder()
                .userId(userId).name("Entertainment").type("EXPENSE").icon("🎬").color("#96CEB4").isDefault(true).build());
        categoryRepository.save(Category.builder()
                .userId(userId).name("Healthcare").type("EXPENSE").icon("🏥").color("#FFEAA7").isDefault(true).build());
        categoryRepository.save(Category.builder()
                .userId(userId).name("Education").type("EXPENSE").icon("📚").color("#DDA0DD").isDefault(true).build());
        categoryRepository.save(Category.builder()
                .userId(userId).name("Bills & Utilities").type("EXPENSE").icon("💡").color("#98D8C8").isDefault(true).build());
        categoryRepository.save(Category.builder()
                .userId(userId).name("Housing").type("EXPENSE").icon("🏠").color("#F7DC6F").isDefault(true).build());
        categoryRepository.save(Category.builder()
                .userId(userId).name("Other").type("EXPENSE").icon("📦").color("#B8B8B8").isDefault(true).build());

        // Default income categories
        categoryRepository.save(Category.builder()
                .userId(userId).name("Salary").type("INCOME").icon("💰").color("#2ECC71").isDefault(true).build());
        categoryRepository.save(Category.builder()
                .userId(userId).name("Freelance").type("INCOME").icon("💻").color("#3498DB").isDefault(true).build());
        categoryRepository.save(Category.builder()
                .userId(userId).name("Investment").type("INCOME").icon("📈").color("#9B59B6").isDefault(true).build());
        categoryRepository.save(Category.builder()
                .userId(userId).name("Other Income").type("INCOME").icon("💵").color("#1ABC9C").isDefault(true).build());
    }
}
