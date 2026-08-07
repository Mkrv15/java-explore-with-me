package ru.practicum.ewm.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.error.model.AccessException;
import ru.practicum.ewm.error.model.ConflictException;
import ru.practicum.ewm.error.model.NotFoundException;
import ru.practicum.ewm.error.model.ValidationException;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventRequest;
import ru.practicum.ewm.event.dto.filter.AdminEventFilter;
import ru.practicum.ewm.event.dto.filter.PublicEventFilter;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.event.model.StateAction;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.stats.client.StatsClient;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.ViewStatsDto;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final EventMapper mapper;
    private final StatsClient statsClient;

    private static final String APP_NAME = "ewm-main-service";

    @Override
    public List<EventFullDto> getEventsAdmin(AdminEventFilter filter) {
        Pageable pageable = PageRequest.of(filter.getFrom() / filter.getSize(), filter.getSize());

        List<EventState> states = filter.getStates();

        List<Event> events = eventRepository.findAllByAdminFilters(
                filter.getUsers(),
                states,
                filter.getCategories(),
                filter.getRangeStart(),
                filter.getRangeEnd(),
                pageable);

        Map<Long, Long> viewsMap = getViews(events);
        return events.stream()
                .map(event -> {
                    EventFullDto dto = mapper.toEventFullDto(event);
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventRequest request) {
        Event event = getEventOrThrow(eventId);

        if (request.getEventDate() != null) {
            if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ValidationException("Дата начала события должна быть не ранее чем за час от даты публикации.");
            }
        }

        if (request.getStateAction() != null) {
            if (request.getStateAction() == StateAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие можно публиковать, только если оно в состоянии PENDING.");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (request.getStateAction() == StateAction.REJECT_EVENT) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Событие нельзя отклонить, так как оно уже опубликовано.");
                }
                event.setState(EventState.CANCELED);
            }
        }
        updateEventFields(event, request);
        Event saved = eventRepository.save(event);

        EventFullDto dto = mapper.toEventFullDto(saved);
        dto.setViews(getViews(List.of(saved)).getOrDefault(eventId, 0L));
        return dto;
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата и время события не могут быть раньше," +
                    " чем через 2 часа от текущего момента.");
        }
        User initiator = getUserOrThrow(userId);
        Category category = getCategoryOrThrow(newEventDto.getCategory());

        Event event = mapper.mapToEvent(newEventDto);
        event.setInitiator(initiator);
        event.setCategory(category);
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());
        event.setConfirmedRequests(0);

        Event saved = eventRepository.save(event);
        EventFullDto dto = mapper.toEventFullDto(saved);
        dto.setViews(0L);
        return dto;
    }

    @Override
    @Transactional
    public EventFullDto updateEventUser(Long userId, Long eventId, UpdateEventRequest request) {
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new AccessException("User don't have permission to update this event");
        }

        if (request.getEventDate() != null) {
            if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Дата и время намечающегося события не может быть раньше, чем через два часа от текущего момента.");
            }
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя изменять опубликованное событие");
        }

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case SEND_TO_REVIEW:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Нельзя отправить на модерацию опубликованное событие");
                    }
                    event.setState(EventState.PENDING);
                    break;

                case CANCEL_REVIEW:
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Отменить можно только событие в состоянии ожидания модерации (PENDING)");
                    }
                    event.setState(EventState.CANCELED);
                    break;

                default:
                    break;
            }
        }


        updateEventFields(event, request);
        Event saved = eventRepository.save(event);

        EventFullDto dto = mapper.toEventFullDto(saved);
        dto.setViews(getViews(List.of(saved)).getOrDefault(eventId, 0L));
        return dto;
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        Map<Long, Long> viewsMap = getViews(events);
        return events.stream()
                .map(event -> {
                    EventShortDto dto = mapper.toEventShortDto(event);
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        EventFullDto dto = mapper.toEventFullDto(event);
        dto.setViews(getViews(List.of(event)).getOrDefault(eventId, 0L));
        return dto;
    }

    @Override
    public List<EventShortDto> getEventsPublic(PublicEventFilter filter, HttpServletRequest request) {
        LocalDateTime start = (filter.getRangeStart() != null) ? filter.getRangeStart() : LocalDateTime.now();

        Pageable pageable = PageRequest.of(filter.getFrom() / filter.getSize(), filter.getSize());

        List<Long> categories = filter.getCategories();

        List<Event> events;
        if (categories == null || categories.isEmpty()) {
            events = eventRepository.findAllByPublicFiltersWithoutCategories(
                    filter.getText(),
                    filter.getPaid(),
                    start,
                    filter.getRangeEnd(),
                    EventState.PUBLISHED.name(),
                    pageable
            );
        } else {
            events = eventRepository.findAllByPublicFiltersWithCategories(
                    filter.getText(),
                    categories,
                    filter.getPaid(),
                    start,
                    filter.getRangeEnd(),
                    EventState.PUBLISHED.name(),
                    pageable
            );
        }

        if (filter.getOnlyAvailable() != null && filter.getOnlyAvailable()) {
            events = events.stream()
                    .filter(event ->
                            event.getParticipantLimit() == 0 ||
                                    event.getConfirmedRequests() < event.getParticipantLimit())
                    .collect(Collectors.toList());
        }

        saveHit(request.getRequestURI(), request.getRemoteAddr());

        Map<Long, Long> viewsMap = getViews(events);

        List<EventShortDto> dtos = events.stream()
                .map(event -> {
                    EventShortDto dto = mapper.toEventShortDto(event);
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());

        if (filter.getSort() != null) {
            if (filter.getSort().equalsIgnoreCase("EVENT_DATE")) {
                dtos.sort(Comparator.comparing(EventShortDto::getEventDate));
            } else if (filter.getSort().equalsIgnoreCase("VIEWS")) {
                dtos.sort(Comparator.comparing(EventShortDto::getViews,
                        Comparator.nullsLast(Comparator.reverseOrder())));
            }
        }
        return dtos;
    }

    @Override
    public EventFullDto getEventByIdPublic(Long eventId, HttpServletRequest request) {
        Event event = getEventOrThrow(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        saveHit(request.getRequestURI(), request.getRemoteAddr());


        Map<Long, Long> viewsMap = getViews(List.of(event));
        Long views = viewsMap.getOrDefault(event.getId(), 0L);

        if (views == 0) views = 1L;

        EventFullDto dto = mapper.toEventFullDto(event);
        dto.setViews(views);
        return dto;
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Только инициатор события может изменять запросы на участие.");
        }

        if (event.getParticipantLimit() != 0 && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("Лимит одобренных заявок достигнут.");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(updateRequest.getRequestIds());

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();

        int confirmedCount = event.getConfirmedRequests();
        long limit = event.getParticipantLimit();

        for (ParticipationRequest request : requests) {
            if (!request.getRequestStatus().equals(RequestStatus.PENDING)) {
                throw new ConflictException("Статус может быть изменён только для PENDING заявок.");
            }

            if (updateRequest.getStatus().equals(RequestStatus.CONFIRMED)) {
                if (limit == 0 || confirmedCount < limit) {
                    request.setRequestStatus(RequestStatus.CONFIRMED);
                    confirmedCount++;
                    result.getConfirmedRequests().add(convertToRequestDto(request));
                } else {
                    request.setRequestStatus(RequestStatus.REJECTED);
                    result.getRejectedRequests().add(convertToRequestDto(request));
                }
            } else {
                request.setRequestStatus(RequestStatus.REJECTED);
                result.getRejectedRequests().add(convertToRequestDto(request));
            }
        }

        event.setConfirmedRequests(confirmedCount);
        requestRepository.saveAll(requests);

        return result;
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Только инициатор события может просматривать запросы на участие.");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByEventId(eventId);

        return requests.stream()
                .map(this::convertToRequestDto)
                .collect(Collectors.toList());
    }


    private Map<Long, Long> getViews(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .toList();

        LocalDateTime start = events.stream()
                .map(Event::getPublishedOn)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusDays(30));

        LocalDateTime end = LocalDateTime.now();
        Map<Long, Long> viewsMap = new HashMap<>();

        try {
            List<ViewStatsDto> stats = statsClient.getStats(start, end, uris, true);

            for (ViewStatsDto stat : stats) {
                String uri = stat.getUri();
                String idStr = uri.substring(uri.lastIndexOf("/") + 1);
                Long eventId = Long.parseLong(idStr);
                viewsMap.put(eventId, stat.getHits());
            }
        } catch (Exception e) {
            log.error("Ошибка при получении статистики просмотров: {}", e.getMessage());
        }
        return viewsMap;
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + categoryId + " was not found"));
    }

    private void updateEventFields(Event event, UpdateEventRequest request) {
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getLocation() != null) {
            event.setLocation(new Location(request.getLocation().getLat(), request.getLocation().getLon()));
        }
        if (request.getCategory() != null) {
            Category category = getCategoryOrThrow(request.getCategory());
            event.setCategory(category);
        }
    }

    private ParticipationRequestDto convertToRequestDto(ParticipationRequest request) {
        ParticipationRequestDto dto = new ParticipationRequestDto();
        dto.setId(request.getId());
        dto.setEvent(request.getEvent().getId());
        dto.setRequester(request.getRequester().getId());
        dto.setStatus(request.getRequestStatus().toString());
        dto.setCreated(request.getCreated());
        return dto;
    }

    private void saveHit(String uri, String ip) {
        try {
            EndpointHitDto hit = new EndpointHitDto();
            hit.setApp(APP_NAME);
            hit.setUri(uri);
            hit.setIp(ip);
            hit.setTimestamp(LocalDateTime.now());
            statsClient.addHit(hit);
        } catch (Exception e) {
            log.error("Не удалось сохранить хит: {}", e.getMessage());
        }
    }
}
