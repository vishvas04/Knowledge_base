package com.knowledge.knowledge_support_tool.service;

import com.knowledge.knowledge_support_tool.repository.QueryLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricsService {

    @Autowired
    private QueryLogRepository queryLogRepository;

    public Map<String, Object> getDailyMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("queryVolume", queryLogRepository.countQueriesPerDay());
        metrics.put("averageResponseTime", queryLogRepository.findAverageResponseTime());
        metrics.put("successRate", queryLogRepository.findSuccessRate());
        return metrics;
    }

    public List<Object[]> getMostQueriedDocuments() {
        return queryLogRepository.findTopReferencedDocuments();
    }

    public Map<String, Double> getPerformanceStatistics() {
        return Map.of(
                "averageResponseTime", queryLogRepository.findAverageResponseTime(),
                "successRate", queryLogRepository.findSuccessRate()
        );
    }

}
