package com.refind.controller;

import com.refind.dto.common.PublicStatsResponse;
import com.refind.service.StatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/public")
    public ResponseEntity<PublicStatsResponse> getPublicStats() {
        return ResponseEntity.ok(statisticsService.getPublicStats());
    }
}
