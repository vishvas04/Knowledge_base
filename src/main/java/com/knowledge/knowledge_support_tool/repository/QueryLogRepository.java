package com.knowledge.knowledge_support_tool.repository;

import com.knowledge.knowledge_support_tool.model.QueryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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
    SELECT DATE_TRUNC('day', timestamp) AS day, COUNT(*) 
    FROM query_log 
    WHERE timestamp BETWEEN :start AND :end 
    GROUP BY day""",
            nativeQuery = true)
    List<Object[]> countQueriesPerDay(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value = """
    SELECT unnested_doc, COUNT(*) as count 
    FROM (
        SELECT UNNEST(referenced_documents) AS unnested_doc 
        FROM query_log
    ) AS subquery 
    GROUP BY unnested_doc 
    ORDER BY count DESC 
    LIMIT 5
    """, nativeQuery = true)
    List<Object[]> findTopReferencedDocuments();
}
