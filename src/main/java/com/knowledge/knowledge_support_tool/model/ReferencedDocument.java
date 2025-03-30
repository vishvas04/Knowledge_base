package com.knowledge.knowledge_support_tool.model;

import jakarta.persistence.*;

@Entity
public class ReferencedDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String documentId;  // Stores the ID of the referenced document

    @ManyToOne
    @JoinColumn(name = "query_log_id")
    private QueryLog queryLog;

    // Getters and Setters
    public Long getId() { return id; }
    public String getDocumentId() { return documentId; }
    public QueryLog getQueryLog() { return queryLog; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setQueryLog(QueryLog queryLog) { this.queryLog = queryLog; }
}