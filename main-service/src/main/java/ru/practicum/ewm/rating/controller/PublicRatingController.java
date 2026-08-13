package ru.practicum.ewm.rating.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.rating.dto.EventRatingDto;
import ru.practicum.ewm.rating.dto.RatingDto;
import ru.practicum.ewm.rating.dto.UserRatingDto;
import ru.practicum.ewm.rating.service.RatingService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PublicRatingController {

    private final RatingService ratingService;

    @GetMapping("/events/{eventId}/ratings")
    public List<RatingDto> getEventRatings(@PathVariable Long eventId) {
        return ratingService.getEventReactions(eventId);
    }

    @GetMapping("/events/{eventId}/rating")
    public EventRatingDto getEventRating(@PathVariable Long eventId) {
        return ratingService.getEventRating(eventId);
    }

    @GetMapping("/users/{userId}/rating")
    public UserRatingDto getUserRating(@PathVariable Long userId) {
        return ratingService.getUserRating(userId);
    }
}
