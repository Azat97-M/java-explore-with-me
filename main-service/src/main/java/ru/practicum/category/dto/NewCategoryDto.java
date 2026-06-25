package ru.practicum.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCategoryDto {

    @NotBlank(message = "Заполните название категории")
    @Size(min = 1, max = 50, message = "Название должно быть от 1 до 50 символов")
    private String name;
}
