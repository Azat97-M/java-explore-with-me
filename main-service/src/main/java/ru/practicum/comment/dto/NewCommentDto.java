package ru.practicum.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCommentDto {

    @NotBlank(message = "Заполните текст комментария")
    @Size(min = 1, max = 2000, message = "Текст комментария должен быть от 1 до 2000 символов")
    private String text;
}
