package com.knowledge.knowledge_support_tool.controller;

import com.knowledge.knowledge_support_tool.service.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    @Autowired
    private MetricsService metricsService;

    @GetMapping("/daily")
    public ResponseEntity<Map<String, Object>> getDailyMetrics() {
        return ResponseEntity.ok(metricsService.getDailyMetrics());
    }

    @GetMapping("/documents/top")
    public ResponseEntity<List<Object[]>> getTopDocuments() {
        return ResponseEntity.ok(metricsService.getMostQueriedDocuments());
    }

    @GetMapping("/performance")
    public ResponseEntity<Map<String, Double>> getPerformanceMetrics() {
        return ResponseEntity.ok(metricsService.getPerformanceStatistics());
    }
}
