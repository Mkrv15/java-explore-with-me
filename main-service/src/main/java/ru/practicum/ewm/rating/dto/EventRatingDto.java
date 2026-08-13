package ru.practicum.ewm.rating.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRatingDto {

    private Long eventId;
    private String title;
    private Integer likes;
    private Integer dislikes;
    private Integer rating;
    private Double ratingPercent;
}
