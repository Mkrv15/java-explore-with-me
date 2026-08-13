package ru.practicum.ewm.rating.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.rating.dto.RatingDto;
import ru.practicum.ewm.rating.model.Rating;
import ru.practicum.ewm.user.mapper.UserMapper;

@Mapper(componentModel = "spring", uses = {EventMapper.class, UserMapper.class})
public interface RatingMapper {

    @Mapping(target = "event", source = "event")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "createdAt", source = "createdAt")
    RatingDto toDto(Rating rating);
}
