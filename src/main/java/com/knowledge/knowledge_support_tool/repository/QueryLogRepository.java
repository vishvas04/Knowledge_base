package com.knowledge.knowledge_support_tool.repository;

import com.knowledge.knowledge_support_tool.model.QueryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QueryLogRepository extends JpaRepository<QueryLog, Long> {

    @Query(value = "SELECT DATE_TRUNC('day', timestamp) AS day, COUNT(*) FROM query_log GROUP BY day",
            nativeQuery = true)
    List<Object[]> countQueriesPerDay();

    @Query("SELECT AVG(responseTime) FROM QueryLog WHERE success = true")
    Double findAverageResponseTime();

    @Query("SELECT (COUNT(*) FILTER (WHERE success = true) * 100.0) / COUNT(*) FROM QueryLog")
    Double findSuccessRate();

    @Query(value = """
            SELECT referenced_documents, COUNT(*) as count 
            FROM query_log 
            GROUP BY referenced_documents 
            ORDER BY count DESC 
            LIMIT 5
            """, nativeQuery = true)
    List<Object[]> findTopReferencedDocuments();
}
