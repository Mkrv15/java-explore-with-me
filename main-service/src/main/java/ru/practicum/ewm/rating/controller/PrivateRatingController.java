package ru.practicum.ewm.rating.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.rating.dto.CreateReactionRequest;
import ru.practicum.ewm.rating.dto.RatingDto;
import ru.practicum.ewm.rating.service.RatingService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/ratings")
@RequiredArgsConstructor
@Slf4j
public class PrivateRatingController {

    private final RatingService ratingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RatingDto addRating(@PathVariable Long userId, @RequestBody @Valid CreateReactionRequest request) {
        log.info("User {} reacting to event {}, isLike={}", userId, request.getEventId(), request.getIsLike());
        return ratingService.addRating(userId, request);
    }

    @PatchMapping("/{ratingId}")
    public RatingDto updateRating(@PathVariable Long userId,
                                  @PathVariable Long ratingId,
                                  @RequestParam Boolean isLike){
        log.info("User {} updating reaction {} to isLike={}", userId, ratingId, isLike);
        return ratingService.updateRating(userId, ratingId, isLike);
    }

    @DeleteMapping("/{ratingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRating(@PathVariable Long userId, @PathVariable Long ratingId){
        log.info("User {} removing reaction {}", userId, ratingId);
        ratingService.deleteRating(userId,ratingId);
    }

    @GetMapping
    public List<RatingDto> getUserRatings(@PathVariable Long userId){
        log.debug("Getting rating for user {} ", userId);
        return ratingService.getUserRatings(userId);
    }

    @GetMapping("/event/{eventId}")
    public RatingDto getEventRating(@PathVariable Long userId, @PathVariable Long eventId){
        log.info("Getting reaction for user {} and event {}", userId, eventId);
        return ratingService.getRatingByEventAndUser(eventId, userId);
    }
}
