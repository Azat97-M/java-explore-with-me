package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.EndpointHitDto;
import ru.practicum.StatsClient;
import ru.practicum.category.dal.CategoryRepository;
import ru.practicum.category.model.Category;
import ru.practicum.errorHandler.ConflictException;
import ru.practicum.event.dal.EventRepository;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.errorHandler.NotFoundException;
import ru.practicum.request.dal.RequestRepository;
import ru.practicum.user.dal.UserRepository;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatsClient statsClient;
    private final RequestRepository requestRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("Создание события пользователем id={}", userId);

        validateEventDate(newEventDto.getEventDate(), 2);

        User initiator = getUserOrThrow(userId);
        Category category = getCategoryOrThrow(newEventDto.getCategory());

        Event event = Event.builder()
                .annotation(newEventDto.getAnnotation())
                .category(category)
                .confirmedRequests(0L)
                .createdOn(LocalDateTime.now())
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .initiator(initiator)
                .location(newEventDto.getLocation())
                .paid(newEventDto.getPaid())
                .participantLimit(newEventDto.getParticipantLimit())
                .requestModeration(newEventDto.getRequestModeration())
                .state(EventState.PENDING)
                .title(newEventDto.getTitle())
                .views(0L)
                .build();

        Event savedEvent = eventRepository.save(event);
        return EventMapper.toEventFullDto(savedEvent);
    }

    @Override
    public List<EventShortDto> getEventsByUser(Long userId, int from, int size) {
        log.info("Получение событий пользователя id={}", userId);

        Pageable pageable = PageRequest.of(from / size, size);

        return eventRepository.findByInitiatorId(userId, pageable).stream()
                .map(EventMapper::toEventShortDto)
                .toList();
    }

    @Override
    public EventFullDto getEventByUserAndId(Long userId, Long eventId) {
        log.info("Получение детальной информации о событии id={} пользователем id={}", eventId, userId);

        Event event = getEventByInitiatorOrThrow(eventId, userId);
        return EventMapper.toEventFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.info("Обновление события id={} пользователем id={}", eventId, userId);

        Event event = getEventByInitiatorOrThrow(eventId, userId);
        checkEventCanBeModified(event);

        if (request.getEventDate() != null) {
            validateEventDate(request.getEventDate(), 2);
            event.setEventDate(request.getEventDate());
        }

        updateFieldsFromUserRequest(event, request);
        updateEventStateFromUserAction(event, request.getStateAction());

        Event updatedEvent = eventRepository.save(event);
        return EventMapper.toEventFullDto(updatedEvent);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }

    private Category getCategoryOrThrow(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + catId + " не найдена"));
    }

    private Event getEventByInitiatorOrThrow(Long eventId, Long userId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));
    }

    private void validateEventDate(LocalDateTime date, int hours) {
        if (date.isBefore(LocalDateTime.now().plusHours(hours))) {
            throw new IllegalArgumentException("Дата события должна быть на " + hours + " часа позже");
        }
    }

    private void checkEventCanBeModified(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя изменить уже опубликованное событие");
        }
    }

    private void updateFieldsFromUserRequest(Event event, UpdateEventUserRequest request) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getLocation() != null) event.setLocation(request.getLocation());
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());

        if (request.getCategory() != null) {
            event.setCategory(getCategoryOrThrow(request.getCategory()));
        }
    }

    private void updateEventStateFromUserAction(Event event, UserStateAction action) {
        if (action == null) {
            return;
        }
        if (action == UserStateAction.SEND_TO_REVIEW) {
            event.setState(EventState.PENDING);
        } else if (action == UserStateAction.CANCEL_REVIEW) {
            event.setState(EventState.CANCELED);
        }
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> statesStr, List<Long> categories,
                                               String rangeStart, String rangeEnd, int from, int size) {
        LocalDateTime start = parseDateTimeOrDefault(rangeStart, null);
        LocalDateTime end = parseDateTimeOrDefault(rangeEnd, null);

        List<EventState> states = null;
        if (statesStr != null && !statesStr.isEmpty()) {
            states = statesStr.stream()
                    .map(EventState::valueOf)
                    .toList();
        }

        int pageSize = size;
        if (size <= 0) {
            pageSize = 10;
        }
        Pageable pageable = PageRequest.of(from / pageSize, pageSize);

        List<Event> events = eventRepository.findEventsByAdmin(users, states, categories, start, end, pageable).getContent();

        if (events.isEmpty()) {
            return List.of();
        }

        loadConfirmedRequests(events);
        loadViewsForList(events);

        return events.stream()
                .map(EventMapper::toEventFullDto)
                .toList();
    }


    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        log.info("Обновление события id={} администратором", eventId);

        Event event = getEventOrThrow(eventId);

        if (request.getEventDate() != null) {
            validateEventDate(request.getEventDate(), 1);
            event.setEventDate(request.getEventDate());
        }

        updateFieldsFromAdminRequest(event, request);

        if (request.getStateAction() != null) {
            processAdminStateAction(event, request.getStateAction());
        }

        Event updatedEvent = eventRepository.save(event);
        return EventMapper.toEventFullDto(updatedEvent);
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));
    }

    private void updateFieldsFromAdminRequest(Event event, UpdateEventAdminRequest request) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getLocation() != null) event.setLocation(request.getLocation());
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());

        if (request.getCategory() != null) {
            event.setCategory(getCategoryOrThrow(request.getCategory()));
        }
    }

    private void processAdminStateAction(Event event, AdminStateAction action) {
        if (action == AdminStateAction.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Публиковать можно только события, находящиеся в состоянии ожидания");
            }
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if (action == AdminStateAction.REJECT_EVENT) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Нельзя отклонить уже опубликованное событие");
            }
            event.setState(EventState.CANCELED);
        }
    }

    @Override
    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                               String sort, int from, int size, String ip, String uri) {
        boolean checkOnlyAvailable = isOnlyAvailableSelected(onlyAvailable);

        int pageSize = size;
        if (size <= 0) {
            pageSize = 10;
        }

        Pageable pageable = PageRequest.of(from / pageSize, pageSize);

        List<Event> events;

        if (rangeStart != null || rangeEnd != null) {
            LocalDateTime start = parseDateTimeOrDefault(rangeStart, LocalDateTime.now());
            LocalDateTime end = parseDateTimeOrDefault(rangeEnd, LocalDateTime.now().plusYears(10));
            validateSearchDates(start, end);

            events = eventRepository.findEventsPublicWithDates(
                    text, categories, paid, start, end, checkOnlyAvailable, pageable
            ).getContent();
        } else {
            events = eventRepository.findEventsPublicWithoutDates(
                    text, categories, paid, checkOnlyAvailable, pageable
            ).getContent();
        }

        if (events.isEmpty()) {
            return List.of();
        }

        loadViewsForList(events);

        List<Event> sortedEvents = sortEvents(events, sort);
        sendHitToStatsServer("main-service", uri, ip);

        return sortedEvents.stream()
                .map(EventMapper::toEventShortDto)
                .toList();
    }

    private LocalDateTime parseDateTimeOrDefault(String dateStr, LocalDateTime defaultValue) {
        if (dateStr == null) {
            return defaultValue;
        }
        return LocalDateTime.parse(dateStr, FORMATTER);
    }

    private void validateSearchDates(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата начала поиска не может быть позже даты окончания");
        }
    }

    private boolean isOnlyAvailableSelected(Boolean onlyAvailable) {
        if (onlyAvailable == null) {
            return false;
        }
        return onlyAvailable;
    }

    private List<Event> sortEvents(List<Event> events, String sort) {
        if (sort == null) {
            return events;
        }
        if (sort.equals("EVENT_DATE")) {
            return events.stream()
                    .sorted(Comparator.comparing(Event::getEventDate))
                    .toList();
        }
        if (sort.equals("VIEWS")) {
            return events.stream()
                    .sorted(Comparator.comparing(Event::getViews).reversed())
                    .toList();
        }
        return events;
    }

    @Override
    @Transactional
    public EventFullDto getEventByIdPublic(Long eventId, String ip, String uri) {
        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Опубликованное событие с id " + eventId + " не найдено"));

        sendHitToStatsServer("main-service", uri, ip);
        loadViews(event);

        return EventMapper.toEventFullDto(event);
    }

    private void sendHitToStatsServer(String app, String uri, String ip) {
        try {
            String timestampStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            EndpointHitDto hitDto = EndpointHitDto.builder()
                    .app(app)
                    .uri(uri)
                    .ip(ip)
                    .timestamp(timestampStr)
                    .build();
            statsClient.hit(hitDto);
        } catch (Exception e) {
            log.error("Не удалось отправить запись о просмотре в сервис статистики: {}", e.getMessage());
        }
    }

    private void loadViews(Event event) {
        try {
            LocalDateTime createdDate = event.getCreatedOn();
            if (createdDate == null) {
                createdDate = LocalDateTime.now().minusDays(1);
            }
            String start = createdDate.format(FORMATTER);

            String end = LocalDateTime.now().plusYears(10).format(FORMATTER);

            var response = statsClient.getStats(start, end, List.of("/events/" + event.getId()), true);

            if (response.getBody() instanceof List<?> statsList) {
                if (!statsList.isEmpty()) {
                    event.setViews(1L);
                    return;
                }
            }
            event.setViews(0L);
        } catch (Exception e) {
            event.setViews(0L);
        }
    }

    private void loadViewsForList(List<Event> events) {
        if (events != null) {
            for (Event event : events) {
                loadViews(event);
            }
        }
    }

    private void loadConfirmedRequests(List<Event> events) {
        if (events != null) {
            for (Event event : events) {
                long confirmedCount = requestRepository.countByEventIdAndStatus(event.getId(), ru.practicum.request.model.RequestStatus.CONFIRMED);
                event.setConfirmedRequests(confirmedCount);
            }
        }
    }
}
