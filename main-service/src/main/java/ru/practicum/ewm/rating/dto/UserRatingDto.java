package ru.practicum.ewm.rating.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRatingDto {

    private Long userId;
    private String userName;
    private Integer totalLikes;
    private Integer totalDislikes;
    private Integer rating;
}
