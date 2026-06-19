package ru.practicum.request.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.request.model.ParticipationRequest;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByRequesterId(Long requesterId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long requestId, Long requesterId);

    List<ParticipationRequest> findByEventId(Long eventId);

    List<ParticipationRequest> findByIdIn(List<Long> requestIds);

    long countByEventIdAndStatus(Long eventId, ru.practicum.request.model.RequestStatus status);
}
