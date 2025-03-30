package com.knowledge.knowledge_support_tool.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

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
    @Column(name = "success")
    private Boolean success;

    @OneToMany(
            mappedBy = "queryLog",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ReferencedDocument> referencedDocuments;  // No array annotations
}