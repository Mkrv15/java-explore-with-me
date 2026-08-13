package ru.practicum.ewm.rating.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReactionRequest {

    @NotNull
    private Long eventId;

    @NotNull
    private Boolean isLike;
}
