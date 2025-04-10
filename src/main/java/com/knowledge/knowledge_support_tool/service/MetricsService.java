package com.knowledge.knowledge_support_tool.service;

import com.knowledge.knowledge_support_tool.repository.QueryLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricsService {

    @Autowired
    private QueryLogRepository queryLogRepository;

    public Map<String, Object> getDailyMetrics(LocalDate start, LocalDate end) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("queryVolume", queryLogRepository.countQueriesPerDay(start, end));
        metrics.put("averageResponseTime", queryLogRepository.findAverageResponseTime());
        metrics.put("successRate", queryLogRepository.findSuccessRate());
        return metrics;
    }

    public Map<String, Object> getDailyMetrics() {
        return getDailyMetrics(LocalDate.MIN, LocalDate.MAX); // Default to all time
    }

    public List<Object[]> getMostQueriedDocuments() {
        return queryLogRepository.findTopReferencedDocuments();
    }

    public Map<String, Double> getPerformanceStatistics() {
        Map<String, Object> dailyMetrics = this.getDailyMetrics();
        return Map.of(
                "averageResponseTime", queryLogRepository.findAverageResponseTime(),
                "successRate", queryLogRepository.findSuccessRate()
        );
    }

}
