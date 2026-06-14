package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public void saveHit(EndpointHitDto hitDto) {
        log.info("Сохранение отметки для URI: {}", hitDto.getUri());
        LocalDateTime timestamp = LocalDateTime.parse(hitDto.getTimestamp(), FORMATTER);

        EndpointHit hit = EndpointHit.builder()
                .app(hitDto.getApp())
                .uri(hitDto.getUri())
                .ip(hitDto.getIp())
                .timestamp(timestamp)
                .build();

        statsRepository.save(hit);
    }

    @Override
    public List<ViewStatsDto> getStats(String startStr, String endStr, List<String> uris, boolean unique) {
        log.info("Получение статистики для uris: {}, unique: {}", uris, unique);

        LocalDateTime start = LocalDateTime.parse(startStr, FORMATTER);
        LocalDateTime end = LocalDateTime.parse(endStr, FORMATTER);

        validateDates(start, end);

        return fetchStatsFromRepository(start, end, uris, unique);
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }
    }

    private List<ViewStatsDto> fetchStatsFromRepository(LocalDateTime start, LocalDateTime end,
                                                        List<String> uris, boolean unique) {
        if (uris == null || uris.isEmpty()) {
            if (unique) {
                return statsRepository.getStatsWithoutUrisAndUniqueIp(start, end);
            }
            return statsRepository.getStatsWithoutUrisAndAllIp(start, end);
        }

        if (unique) {
            return statsRepository.getStatsWithUrisAndUniqueIp(start, end, uris);
        }
        return statsRepository.getStatsWithUrisAndAllIp(start, end, uris);
    }
}
