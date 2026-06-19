package ru.practicum.user.dto;

import jakarta.validation.constraints.Email;
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
public class NewUserRequest {

    @NotBlank(message = "Заполните имя")
    @Size(min = 2, max = 250, message = "Имя должно быть от 2 до 250 символов")
    private String name;

    @NotBlank(message = "Заполните Email")
    @Email(message = "Некорректный формат email")
    @Size(min = 6, max = 254, message = "Email должен быть от 6 до 254 символов")
    private String email;
}