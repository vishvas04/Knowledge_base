package com.knowledge.knowledge_support_tool.controller;

import com.knowledge.knowledge_support_tool.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            documentService.saveDocument(file);
            return ResponseEntity.ok("Document uploaded and saved successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Document processing failed: " + e.getMessage());
        }
    }
}
