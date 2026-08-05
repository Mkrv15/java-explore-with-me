package ru.practicum.ewm.event.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @EntityGraph(attributePaths = {"category", "initiator"})
    Optional<Event> findById(Long eventId);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    List<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    @Query(
            value =
                    "SELECT e.* " +
                            "FROM events e " +
                            "WHERE (:text IS NULL OR " +
                            "       e.annotation ILIKE CONCAT('%', :text, '%') OR " +
                            "       e.description ILIKE CONCAT('%', :text, '%')) " +
                            "AND e.category_id IN (:categories) " +
                            "AND (:paid IS NULL OR e.paid = :paid) " +
                            "AND e.event_date >= COALESCE(:rangeStart, e.event_date) " +
                            "AND e.event_date <= COALESCE(:rangeEnd, e.event_date) " +
                            "AND e.state = :state",
            nativeQuery = true
    )
    List<Event> findAllByPublicFiltersWithCategories(
            @Param("text") String text,
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("state") String state,
            Pageable pageable
    );

    @Query(value = "SELECT e.* " +
            "FROM events e " +
            "WHERE (:text IS NULL OR " +
            "       e.annotation ILIKE CONCAT('%', :text, '%') OR " +
            "       e.description ILIKE CONCAT('%', :text, '%')) " +
            "AND (:paid IS NULL OR e.paid = :paid) " +
            "AND e.event_date >= COALESCE(:rangeStart, e.event_date) " +
            "AND e.event_date <= COALESCE(:rangeEnd, e.event_date) " +
            "AND e.state = :state",
            nativeQuery = true
    )
    List<Event> findAllByPublicFiltersWithoutCategories(
            @Param("text") String text,
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("state") String state,
            Pageable pageable
    );

    @Query("SELECT e FROM Event e " +
            "WHERE (:users IS NULL OR e.initiator.id IN :users) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND e.eventDate >= COALESCE(:rangeStart, e.eventDate) " +
            "AND e.eventDate <= COALESCE(:rangeEnd, e.eventDate) " +
            "ORDER BY e.id")
    List<Event> findAllByAdminFilters(
            @Param("users") List<Long> users,
            @Param("states") List<EventState> states,
            @Param("categories") List<Long> categories,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable
    );
}
