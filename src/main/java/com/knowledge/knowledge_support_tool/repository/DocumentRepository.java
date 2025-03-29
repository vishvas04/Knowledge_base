package com.knowledge.knowledge_support_tool.repository;

import com.knowledge.knowledge_support_tool.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}

