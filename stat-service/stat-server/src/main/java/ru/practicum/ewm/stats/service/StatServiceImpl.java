package ru.practicum.ewm.stats.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.ViewStatsDto;
import ru.practicum.ewm.stats.mapper.EndpointHitMapper;
import ru.practicum.ewm.stats.model.EndpointHit;
import ru.practicum.ewm.stats.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {
    private final StatsRepository statsRepository;
    private final EndpointHitMapper mapper;

    @Override
    @Transactional
    public EndpointHitDto saveEndpointHit(EndpointHitDto endpointHitDto) {
        EndpointHit saved = statsRepository.save(mapper.toEndpointHit(endpointHitDto));
        return mapper.toEndpointHitDto(saved);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {

        if (start == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (end == null) {
            throw new IllegalArgumentException("End date cannot be null");
        }

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Дата старта должна быть раньше даты окончания");
        }
        if (end.equals(start)) {
            throw new IllegalArgumentException("Дата старта должна быть раньше даты окончания");
        }

        return unique ? statsRepository.findUniqueStats(start, end, uris)
                : statsRepository.findStats(start, end, uris);
    }
}
