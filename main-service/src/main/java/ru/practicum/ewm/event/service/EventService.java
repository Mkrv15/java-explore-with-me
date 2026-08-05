package ru.practicum.ewm.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventRequest;
import ru.practicum.ewm.event.dto.filter.AdminEventFilter;
import ru.practicum.ewm.event.dto.filter.PublicEventFilter;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;

import java.util.List;

public interface EventService {

    List<EventFullDto> getEventsAdmin(AdminEventFilter filter);

    EventFullDto updateEventAdmin(Long eventId, UpdateEventRequest request);

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    EventFullDto updateEventUser(Long userId, Long eventId, UpdateEventRequest request);

    List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size);

    EventFullDto getUserEventById(Long userId, Long eventId);

    List<EventShortDto> getEventsPublic(PublicEventFilter filter, HttpServletRequest request);

    EventFullDto getEventByIdPublic(Long eventId, HttpServletRequest request);

    EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest updateRequest);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);
}
