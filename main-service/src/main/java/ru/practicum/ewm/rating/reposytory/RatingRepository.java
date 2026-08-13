package ru.practicum.ewm.rating.reposytory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.rating.model.Rating;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserIdAndEventId(Long userId, Long eventId);

    Long countLikesByEventIdAndIsLikeTrue(Long eventId);

    Long countDislikesByEventIdAndIsLikeFalse(Long eventId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.event.initiator.id = :authorId AND r.isLike = true")
    Long countLikesReceivedByAuthor(@Param("authorId") Long authorId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.event.initiator.id = :authorId AND r.isLike = false")
    Long countDislikesReceivedByAuthor(@Param("authorId") Long authorId);

    List<Rating> findAllByUserId(Long userId);

    List<Rating> findAllByEventId(Long eventId);

}
