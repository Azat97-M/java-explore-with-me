package ru.practicum;

import java.util.List;

public interface StatsService {
    void saveHit(EndpointHitDto hitDto);

    List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique);
}