package ru.practicum.ewm.rating.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.error.model.ConflictException;
import ru.practicum.ewm.error.model.NotFoundException;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.rating.dto.CreateReactionRequest;
import ru.practicum.ewm.rating.dto.EventRatingDto;
import ru.practicum.ewm.rating.dto.RatingDto;
import ru.practicum.ewm.rating.dto.UserRatingDto;
import ru.practicum.ewm.rating.mapper.RatingMapper;
import ru.practicum.ewm.rating.model.Rating;
import ru.practicum.ewm.rating.reposytory.RatingRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RatingMapper mapper;

    @Override
    public RatingDto addRating(Long userId, CreateReactionRequest request) {
        User user = getUserOrElseThrow(userId);

        Event event = getEventOrElseThrow(request.getEventId());

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot react to unpublished event");
        }

        if (event.getInitiator().equals(user)) {
            throw new ConflictException("Initiator cannot react to their own event");
        }

        if (ratingRepository.findByUserIdAndEventId(userId, event.getId()).isPresent()) {
            throw new ConflictException("User has already reacted to this event");
        }

        Rating rating = new Rating();
        rating.setUser(user);
        rating.setEvent(event);
        rating.setIsLike(request.getIsLike());
        rating.setCreatedAt(LocalDateTime.now());

        Rating saved = ratingRepository.save(rating);

        updateUserRating(event.getInitiator());
        updateEventRating(event);

        return mapper.toDto(saved);
    }

    @Override
    public RatingDto updateRating(Long userId, Long reactionId, Boolean isLike) {
        User user = getUserOrElseThrow(userId);

        Rating rating = getRatingOrElseThrow(reactionId);

        if (rating.getEvent().getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot react to unpublished event");
        }

        if (rating.getEvent().getInitiator().equals(user)) {
            throw new ConflictException("User the owner of this reaction");
        }

        rating.setIsLike(isLike);

        Rating saved = ratingRepository.save(rating);
        updateUserRating(rating.getEvent().getInitiator());
        updateEventRating(rating.getEvent());

        return mapper.toDto(saved);
    }

    @Override
    public void deleteRating(Long userId, Long reactionId) {
        Rating rating = getRatingOrElseThrow(reactionId);
        User user = getUserOrElseThrow(userId);

        if (rating.getEvent().getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot react to unpublished event");
        }
        if (!rating.getUser().equals(user)) {
            throw new ConflictException("User is not the owner of this reaction");
        }

        ratingRepository.delete(rating);
        updateUserRating(rating.getEvent().getInitiator());
        updateEventRating(rating.getEvent());
    }

    @Override
    public RatingDto getRatingByEventAndUser(Long eventId, Long userId) {
        Rating rating = ratingRepository.findByUserIdAndEventId(userId, eventId).orElseThrow(
                () -> new NotFoundException("Reaction not found for event=" + eventId + " and user=" + userId));
        return mapper.toDto(rating);
    }

    @Override
    public List<RatingDto> getUserRatings(Long userId) {
        User user = getUserOrElseThrow(userId);
        return ratingRepository.findAllByUserId(user.getId()).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<RatingDto> getEventReactions(Long eventId) {
        Event event = getEventOrElseThrow(eventId);
        return ratingRepository.findAllByEventId(event.getId()).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventRatingDto getEventRating(Long eventId) {
        Event event = getEventOrElseThrow(eventId);

        Long likes = ratingRepository.countLikesByEventIdAndIsLikeTrue(eventId);
        Long dislikes = ratingRepository.countDislikesByEventIdAndIsLikeFalse(eventId);

        EventRatingDto eventRatingDto = new EventRatingDto();
        eventRatingDto.setEventId(event.getId());
        eventRatingDto.setDislikes(dislikes.intValue());
        eventRatingDto.setLikes(likes.intValue());
        eventRatingDto.setTitle(event.getTitle());
        eventRatingDto.setRating(eventRatingDto.getLikes() - eventRatingDto.getDislikes());

        int total = eventRatingDto.getLikes() + eventRatingDto.getDislikes();
        eventRatingDto.setRatingPercent(total > 0 ? (double) eventRatingDto.getLikes() / total * 100 : 0.0);

        return eventRatingDto;
    }

    @Override
    public UserRatingDto getUserRating(Long userId) {
        User user = getUserOrElseThrow(userId);

        Long likes = ratingRepository.countLikesReceivedByAuthor(userId);
        Long dislikes = ratingRepository.countDislikesReceivedByAuthor(userId);

        UserRatingDto dto = new UserRatingDto();
        dto.setUserId(userId);
        dto.setUserName(user.getName());
        dto.setTotalLikes(likes != null ? likes.intValue() : 0);
        dto.setTotalDislikes(dislikes != null ? dislikes.intValue() : 0);
        dto.setRating(dto.getTotalLikes() - dto.getTotalDislikes());

        return dto;
    }

    private void updateEventRating(Event event) {
        Long likes = ratingRepository.countLikesByEventIdAndIsLikeTrue(event.getId());
        Long dislikes = ratingRepository.countDislikesByEventIdAndIsLikeFalse(event.getId());

        int likesCount = likes == null ? 0 : likes.intValue();
        int dislikesCount = dislikes == null ? 0 : dislikes.intValue();

        event.setLikesCount(likesCount);
        event.setDislikesCount(dislikesCount);
        event.setRating(likesCount - dislikesCount);

        eventRepository.save(event);
    }

    private void updateUserRating(User user) {
        Long likes = ratingRepository.countLikesReceivedByAuthor(user.getId());
        Long dislikes = ratingRepository.countDislikesReceivedByAuthor(user.getId());

        int likesCount = likes == null ? 0 : likes.intValue();
        int dislikesCount = dislikes == null ? 0 : dislikes.intValue();

        user.setTotalLikes(likesCount);
        user.setTotalDislikes(dislikesCount);
        userRepository.save(user);
    }

    private User getUserOrElseThrow(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("User not found with id: " + userId));
    }

    private Event getEventOrElseThrow(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() ->
                new NotFoundException("Event not found with id: " + eventId));
    }

    private Rating getRatingOrElseThrow(Long ratingId) {
        return ratingRepository.findById(ratingId).orElseThrow(() ->
                new NotFoundException("Rating not found with id: " + ratingId));
    }
}
