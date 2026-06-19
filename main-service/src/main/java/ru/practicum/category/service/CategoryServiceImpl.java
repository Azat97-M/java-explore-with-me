package ru.practicum.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.dal.CategoryRepository;
import ru.practicum.errorHandler.NotFoundException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto request) {
        log.info("Создание новой категории: {}", request.getName());

        Category category = Category.builder()
                .name(request.getName())
                .build();

        Category savedCategory = categoryRepository.save(category);
        return CategoryMapper.toCategoryDto(savedCategory);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, NewCategoryDto request) {
        log.info("Обновление категории: {}", catId);

        Category category = getCategoryOrThrow(catId);

        category.setName(request.getName());
        Category updatedCategory = categoryRepository.save(category);
        return CategoryMapper.toCategoryDto(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        log.info("Удаление категории {}", catId);

        if (!categoryRepository.existsById(catId)) {
            throw new NotFoundException("Категория " + catId + " не найдена");
        }
        categoryRepository.deleteById(catId);
    }

    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        log.info("Получение списка категорий");

        Pageable pageable = PageRequest.of(from / size, size);

        return categoryRepository.findAll(pageable).stream()
                .map(CategoryMapper::toCategoryDto)
                .toList();
    }

    @Override
    public CategoryDto getCategoryById(Long catId) {
        log.info("Получение категории: {}", catId);

        Category category = getCategoryOrThrow(catId);

        return CategoryMapper.toCategoryDto(category);
    }

    private Category getCategoryOrThrow(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория" + catId + " не найдена"));
    }
}
