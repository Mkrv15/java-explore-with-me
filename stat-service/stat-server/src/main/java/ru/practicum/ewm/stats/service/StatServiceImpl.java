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
@Transactional(readOnly = true)
public class StatServiceImpl implements StatService {
    private final StatsRepository statsRepository;
    private final EndpointHitMapper mapper;

    @Override
    public EndpointHitDto saveEndpointHit(EndpointHitDto endpointHitDto) {
        EndpointHit saved = statsRepository.save(mapper.toEndpointHit(endpointHitDto));
        return mapper.toEndpointHitDto(saved);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        return unique ? statsRepository.findUniqueStats(start, end, uris)
                : statsRepository.findStats(start, end, uris);
    }
}
