package ru.practicum.event.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    @NotNull(message = "Заполните широту")
    private Float lat;

    @NotNull(message = "Заполните долготу")
    private Float lon;
}
