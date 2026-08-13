package ru.practicum.ewm.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.dto.UserShortDto;
import ru.practicum.ewm.user.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "totalLikes", source = "totalLikes")
    @Mapping(target = "totalDislikes", source = "totalDislikes")
    @Mapping(target = "authorRating", source = "authorRating")
    UserDto userToUserDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalLikes", ignore = true)
    @Mapping(target = "totalDislikes", ignore = true)
    @Mapping(target = "authorRating", ignore = true)
    User userDtoToUser(NewUserRequest request);

    @Mapping(target = "totalLikes", source = "totalLikes")
    @Mapping(target = "totalDislikes", source = "totalDislikes")
    @Mapping(target = "authorRating", source = "authorRating")
    UserShortDto userToUserShortDto(User user);
}
