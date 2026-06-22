package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.errorHandler.ConflictException;
import ru.practicum.errorHandler.NotFoundException;
import ru.practicum.event.dal.EventRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.request.dal.RequestRepository;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.user.dal.UserRepository;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Создание запроса. Пользователь id={}", userId);

        User requester = getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        checkRequestConstraints(userId, event);

        RequestStatus initialStatus = RequestStatus.PENDING;
        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            initialStatus = RequestStatus.CONFIRMED;
            incrementConfirmedRequests(event);
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(requester)
                .status(initialStatus)
                .build();

        ParticipationRequest savedRequest = requestRepository.save(request);
        return RequestMapper.toParticipationRequestDto(savedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByUser(Long userId) {
        log.info("Получение заявок пользователя id={}", userId);

        return requestRepository.findByRequesterId(userId).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена заявки id={} пользователем id={}", requestId, userId);

        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Заявка с id " + requestId + " не найдена"));

        if (request.getStatus() == RequestStatus.CONFIRMED) {
            decrementConfirmedRequests(request.getEvent());
        }

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);
        return RequestMapper.toParticipationRequestDto(updatedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByEventOwner(Long userId, Long eventId) {
        log.info("Получение заявок на событие id={} владельцем id={}", eventId, userId);

        return requestRepository.findByEventId(eventId).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest request) {
        log.info("Обновление статусов заявок для события id={} владельцем id={}", eventId, userId);

        Event event = getEventOrThrow(eventId);
        List<ParticipationRequest> requests = requestRepository.findByIdIn(request.getRequestIds());

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (ParticipationRequest req : requests) {
            validatePendingStatus(req);

            if (request.getStatus() == RequestStatus.REJECTED) {
                processRejection(req, rejected);
            } else if (request.getStatus() == RequestStatus.CONFIRMED) {
                processConfirmation(req, event, confirmed, rejected);
            }
        }

        requestRepository.saveAll(requests);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }

    private void validatePendingStatus(ParticipationRequest request) {
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ConflictException("Статус можно менять только у заявок в состоянии PENDING");
        }
    }

    private void processRejection(ParticipationRequest req, List<ParticipationRequestDto> rejected) {
        req.setStatus(RequestStatus.REJECTED);
        rejected.add(RequestMapper.toParticipationRequestDto(req));
    }

    private void processConfirmation(ParticipationRequest req, Event event,
                                     List<ParticipationRequestDto> confirmed,
                                     List<ParticipationRequestDto> rejected) {
        if (isParticipantLimitReached(event)) {
            req.setStatus(RequestStatus.REJECTED);
            rejected.add(RequestMapper.toParticipationRequestDto(req));
            throw new ConflictException("Достигнут лимит мест на данное событие");
        }

        req.setStatus(RequestStatus.CONFIRMED);
        incrementConfirmedRequests(event);
        confirmed.add(RequestMapper.toParticipationRequestDto(req));
    }

    private boolean isParticipantLimitReached(Event event) {
        if (event.getParticipantLimit() > 0) {
            if (event.getConfirmedRequests() >= event.getParticipantLimit()) {
                return true;
            }
        }
        return false;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));
    }

    private void checkRequestConstraints(Long userId, Event event) {
        if (requestRepository.existsByEventIdAndRequesterId(event.getId(), userId)) {
            throw new ConflictException("Нельзя добавить повторный запрос на участие");
        }
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }
        if (event.getParticipantLimit() > 0 && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("У события достигнут лимит запросов на участие");
        }
    }

    private void incrementConfirmedRequests(Event event) {
        event.setConfirmedRequests(event.getConfirmedRequests() + 1);
        eventRepository.save(event);
    }

    private void decrementConfirmedRequests(Event event) {
        if (event.getConfirmedRequests() > 0) {
            event.setConfirmedRequests(event.getConfirmedRequests() - 1);
            eventRepository.save(event);
        }
    }
}

