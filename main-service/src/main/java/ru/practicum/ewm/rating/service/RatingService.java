package ru.practicum.ewm.rating.service;

import ru.practicum.ewm.rating.dto.CreateReactionRequest;
import ru.practicum.ewm.rating.dto.EventRatingDto;
import ru.practicum.ewm.rating.dto.RatingDto;
import ru.practicum.ewm.rating.dto.UserRatingDto;

import java.util.List;

public interface RatingService {

    RatingDto addRating(Long userId, CreateReactionRequest request);

    RatingDto updateRating(Long userId, Long reactionId, Boolean isLike);

    void deleteRating(Long userId, Long reactionId);

    RatingDto getRatingByEventAndUser(Long eventId, Long userId);

    List<RatingDto> getUserRatings(Long userId);

    List<RatingDto> getEventReactions(Long eventId);

    EventRatingDto getEventRating(Long eventId);

    UserRatingDto getUserRating(Long userId);
}
