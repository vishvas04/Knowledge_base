package com.knowledge.knowledge_support_tool.service;

import com.knowledge.knowledge_support_tool.model.Document;
import com.knowledge.knowledge_support_tool.model.QueryLog;
import com.knowledge.knowledge_support_tool.repository.DocumentRepository;
import com.knowledge.knowledge_support_tool.repository.QueryLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class QueryService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private QueryLogRepository queryLogRepository;
    @Autowired
    private DocumentRepository documentRepository;

    @Value("${python.service.url}")
    private String pythonServiceUrl;
    @Value("${file.storage.path}")
    private String fileStoragePath;

//    public String processDocument(MultipartFile file) throws IOException {
//        // Save document locally first
//        Document doc = saveLocalDocument(file);
//
//        // Send to Python processing
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//
//        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//        body.add("file", new ByteArrayResource(file.getBytes()) {
//            @Override
//            public String getFilename() {
//                return file.getOriginalFilename();
//            }
//        });
//
//        String response = restTemplate.postForObject(
//                pythonServiceUrl + "/process-document",
//                new HttpEntity<>(body, headers),
//                String.class
//        );
//
//        if (!"processed".equals(response)) {
//            throw new DocumentProcessingException("Failed to process document");
//        }
//
//        return doc.getId().toString();
//    }
//
//    private Document saveLocalDocument(MultipartFile file) throws IOException {
//        Document doc = new Document();
//        doc.setTitle(file.getOriginalFilename());
//        doc.setFileType(file.getContentType());
//        doc.setFilePath(fileStoragePath + "/" + file.getOriginalFilename());
//        doc.setUploadDate(LocalDateTime.now());
//
//        // Save file to filesystem
//        file.transferTo(new java.io.File(doc.getFilePath()));
//
//        return documentRepository.save(doc);
//    }

    public String processQuery(String question) {
        long startTime = System.currentTimeMillis();
        QueryLog log = new QueryLog();
        log.setQuery(question);
        log.setTimestamp(LocalDateTime.now());

        try {
//            Map<String, String> request = Map.of("question", question);
//            Map<String, Object> result = restTemplate.getForObject(
//                    pythonServiceUrl + "/answer?question=" + question,
//                    Map.class
//            );
            Map<String, String> request = Map.of("question", question);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            Map<String, Object> result = restTemplate.postForObject(
                    pythonServiceUrl + "/answer",
                    entity,
                    Map.class
            );


            log.setResponseTime((System.currentTimeMillis() - startTime) / 1000.0);
            log.setLlmUsed(result.get("llm_used").toString());
            log.setSuccess(true);
            log.setReferencedDocuments((List<String>) result.get("sources"));

            return result.get("answer").toString();
        } catch (Exception e) {
            log.setSuccess(false);
            throw new QueryProcessingException("Query failed: " + e.getMessage());
        } finally {
            queryLogRepository.save(log);
        }
    }

//    // Custom Exceptions
//    public static class DocumentProcessingException extends RuntimeException {
//        public DocumentProcessingException(String message) {
//            super(message);
//        }
//    }

    public static class QueryProcessingException extends RuntimeException {
        public QueryProcessingException(String message) {
            super(message);
        }
    }
}
