package com.knowledge.knowledge_support_tool.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ReferencedDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long documentId;  // Field is declared as Long

    @ManyToOne
    @JoinColumn(name = "query_log_id")
    private QueryLog queryLog;

}