package ru.practicum;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHitDto {

    @NotBlank(message = "Заполните идентификатор сервиса")
    private String app;

    @NotBlank(message = "Заполните URI")
    private String uri;

    @NotBlank(message = "Заполните IP адрес")
    private String ip;

    @NotBlank(message = "Заполните дату и время запроса")
    private String timestamp;
}