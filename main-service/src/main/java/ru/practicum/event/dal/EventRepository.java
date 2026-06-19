package ru.practicum.event.dal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByInitiatorId(Long initiatorId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long initiatorId);

    Optional<Event> findByIdAndState(Long eventId, EventState state);

    @Query("SELECT e FROM Event e " +
            "WHERE (:users IS NULL OR e.initiator.id IN :users) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (:start IS NULL OR e.eventDate >= :start) " +
            "AND (:end IS NULL OR e.eventDate <= :end)")
    Page<Event> findEventsByAdmin(@Param("users") List<Long> users,
                                  @Param("states") List<EventState> states,
                                  @Param("categories") List<Long> categories,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end,
                                  Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND (:text IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) " +
            "OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (:paid IS NULL OR e.paid = :paid) " +
            "AND (e.eventDate >= :start) " +
            "AND (e.eventDate <= :end) " +
            "AND (:onlyAvailable = false OR e.participantLimit = 0 OR e.confirmedRequests < e.participantLimit)")
    Page<Event> findEventsPublicWithDates(@Param("text") String text,
                                          @Param("categories") List<Long> categories,
                                          @Param("paid") Boolean paid,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("onlyAvailable") Boolean onlyAvailable,
                                          Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND (:text IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) " +
            "OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (:paid IS NULL OR e.paid = :paid) " +
            "AND (e.eventDate >= :now) " +
            "AND (:onlyAvailable = false OR e.participantLimit = 0 OR e.confirmedRequests < e.participantLimit)")
    Page<Event> findEventsPublicWithoutDates(@Param("text") String text,
                                             @Param("categories") List<Long> categories,
                                             @Param("paid") Boolean paid,
                                             @Param("now") LocalDateTime now,
                                             @Param("onlyAvailable") Boolean onlyAvailable,
                                             Pageable pageable);
}
