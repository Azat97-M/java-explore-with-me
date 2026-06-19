package ru.practicum.compilation.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.event.mapper.EventMapper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CompilationMapper {

    public static CompilationDto toCompilationDto(Compilation compilation) {
        if (compilation == null) {
            return null;
        }
        return CompilationDto.builder()
                .id(compilation.getId())
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .events(compilation.getEvents().stream()
                        .map(EventMapper::toEventShortDto)
                        .toList())
                .build();
    }
}
