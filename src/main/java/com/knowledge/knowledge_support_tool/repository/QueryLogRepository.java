package com.knowledge.knowledge_support_tool.repository;

import com.knowledge.knowledge_support_tool.model.QueryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface QueryLogRepository extends JpaRepository<QueryLog, Long> {

    @Query(value = """
    SELECT DATE_TRUNC('day', timestamp) AS day, COUNT(*) 
    FROM query_log 
    WHERE timestamp BETWEEN 
        COALESCE(:start, '1970-01-01'::timestamp) AND 
        COALESCE(:end, CURRENT_DATE::timestamp)
    GROUP BY day
    """,
            nativeQuery = true)
    List<Object[]> countQueriesPerDay(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("SELECT AVG(q.responseTime) FROM QueryLog q WHERE q.success = true")
    Double findAverageResponseTime();

    @Query("SELECT (SUM(CASE WHEN q.success = true THEN 1.0 ELSE 0.0 END) / COUNT(q) * 100) FROM QueryLog q")
    Double findSuccessRate();


    @Query(value = """
    SELECT r.document_id AS documentId, 
           COUNT(*) AS count, 
           d.title AS title
    FROM referenced_document r 
    INNER JOIN document d ON r.document_id = d.id
    GROUP BY r.document_id, d.title 
    ORDER BY count DESC 
    LIMIT 5
    """,
            nativeQuery = true)
    List<Object[]> findTopReferencedDocuments();
}