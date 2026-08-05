package ru.practicum.ewm.request.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.user.mapper.UserMapper;

@Mapper(componentModel = "spring", uses = {EventMapper.class, UserMapper.class})
public interface RequestMapper {

    @Mapping(source = "event.id", target = "eventId")
    @Mapping(source = "requester.id", target = "requesterId")
    @Mapping(source = "requestStatus", target = "status")
    ParticipationRequestDto mapToRequestDto(ParticipationRequest participationRequest);
}
