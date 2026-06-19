package ru.practicum.compilation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationDto {

    @Builder.Default
    private List<Long> events = new ArrayList<>();

    @Builder.Default
    private Boolean pinned = false;

    @NotBlank(message = "Заполните заголовок подборки")
    @Size(min = 1, max = 50, message = "Заголовок должен быть от 1 до 50 символов")
    private String title;
}
