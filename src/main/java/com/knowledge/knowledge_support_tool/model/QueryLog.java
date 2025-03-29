package com.knowledge.knowledge_support_tool.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "query_log")
@Getter
@Setter
public class QueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String query;
    private LocalDateTime timestamp;
    private Double responseTime;
    private String llmUsed;
    private Boolean success;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "referenced_documents", columnDefinition = "text[]")
    private List<String> referencedDocuments;
}
