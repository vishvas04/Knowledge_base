package com.knowledge.knowledge_support_tool.service;

import com.knowledge.knowledge_support_tool.model.QueryLog;
import com.knowledge.knowledge_support_tool.model.ReferencedDocument;
import com.knowledge.knowledge_support_tool.repository.QueryLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class QueryService {

    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

    @Autowired
    private QueryLogRepository queryLogRepository;

    @Value("${python.service.url}")
    private String pythonServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String processQuery(String question) {
        long startTime = System.currentTimeMillis();
        QueryLog log = new QueryLog();
        log.setQuery(question);
        log.setTimestamp(LocalDateTime.now());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("question", question), headers);

            Map<String, Object> result = restTemplate.postForObject(
                    pythonServiceUrl + "/answer",
                    entity,
                    Map.class
            );

            log.setResponseTime((System.currentTimeMillis() - startTime) / 1000.0);
            log.setLlmUsed(result.get("llm_used").toString());
            log.setSuccess(true);

            List<String> sources = (List<String>) result.get("sources");
            List<ReferencedDocument> refDocs = sources.stream()
                    .map(documentId -> {
                        ReferencedDocument refDoc = new ReferencedDocument();
                        refDoc.setDocumentId(Long.parseLong(documentId));
                        refDoc.setQueryLog(log);
                        return refDoc;
                    })
                    .toList();
            log.setReferencedDocuments(refDocs);

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