package com.knowledge.knowledge_support_tool.service;

import com.knowledge.knowledge_support_tool.model.Document;
import com.knowledge.knowledge_support_tool.model.QueryLog;
import com.knowledge.knowledge_support_tool.model.ReferencedDocument;
import com.knowledge.knowledge_support_tool.repository.DocumentRepository;
import com.knowledge.knowledge_support_tool.repository.QueryLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

    @Autowired
    private QueryLogRepository queryLogRepository;
    @Autowired
    private DocumentRepository documentRepository;

    @Value("${python.service.url}")
    private String pythonServiceUrl;
    @Value("${file.storage.path}")
    private String fileStoragePath;
    public String processQuery(String question) {
        long startTime = System.currentTimeMillis();
        QueryLog log = new QueryLog();
        log.setQuery(question);
        log.setTimestamp(LocalDateTime.now());

        try {
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

            Object sources = result.get("sources");
            if (sources instanceof List<?>) {
                List<ReferencedDocument> refDocs = ((List<String>) sources).stream()
                        .map(documentIdStr -> {
                            ReferencedDocument refDoc = new ReferencedDocument();
                            refDoc.setDocumentId(Long.parseLong(documentIdStr)); // Parse to Long
                            refDoc.setQueryLog(log);
                            return refDoc;
                        })
                        .toList();
                log.setReferencedDocuments(refDocs);
            } else {
                logger.warn("Unexpected type for 'sources' field: {}",
                        (sources != null) ? sources.getClass() : "null");
            }

            return result.get("answer").toString();
        } catch (Exception e) {
            log.setSuccess(false);
            throw new QueryProcessingException("Query failed: " + e.getMessage());
        } finally {
            queryLogRepository.save(log);
        }
    }

    public static class QueryProcessingException extends RuntimeException {
        public QueryProcessingException(String message) {
            super(message);
        }
    }
}
