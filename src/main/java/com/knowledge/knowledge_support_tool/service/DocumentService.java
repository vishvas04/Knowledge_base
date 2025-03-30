package com.knowledge.knowledge_support_tool.service;

import com.knowledge.knowledge_support_tool.model.Document;
import com.knowledge.knowledge_support_tool.repository.DocumentRepository;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    @Autowired
    private DocumentRepository documentRepository;

    @Value("${file.storage.path}")
    private String storagePath;

    @Value("${python.service.url}")
    private String pythonServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Document saveDocument(MultipartFile file) throws IOException {
        try {
            validateFile(file);

            Path uploadDir = createUploadDirectory();
            Path targetPath = saveFileToStorage(file, uploadDir);
            Document savedDoc = createAndSaveDocument(file, targetPath);

            sendToPythonService(targetPath, savedDoc);
            return savedDoc;

        } catch (Exception e) {
            logger.error("Document processing failed: {}", e.getMessage(), e);
            throw new IOException("Failed to process document: " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() == 0) {
            logger.warn("Attempted to upload an empty file.");
            throw new IOException("Upload failed: The file is empty.");
        }
    }

    private Path createUploadDirectory() throws IOException {
        Path uploadDir = Paths.get(storagePath);
        if (!Files.exists(uploadDir)) {
            logger.info("Creating upload directory: {}", uploadDir);
            Files.createDirectories(uploadDir);
        }
        return uploadDir;
    }

    private Path saveFileToStorage(MultipartFile file, Path uploadDir) throws IOException {
        String originalName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String safeFileName = generateSafeFileName(originalName);
        Path targetPath = uploadDir.resolve(safeFileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        validateFileSavedProperly(targetPath);
        return targetPath;
    }

    private String generateSafeFileName(String originalName) {
        String fileExtension = FilenameUtils.getExtension(originalName);
        String baseName = FilenameUtils.getBaseName(originalName);
        return baseName.replaceAll("[^\\w.-]", "_")
                + "_" + UUID.randomUUID().toString().substring(0, 8)
                + "." + fileExtension;
    }

    private void validateFileSavedProperly(Path targetPath) throws IOException {
        if (!Files.exists(targetPath) || !Files.isReadable(targetPath)) {
            logger.error("File not found or not readable: {}", targetPath);
            throw new IOException("File was not saved properly.");
        }
    }

    private Document createAndSaveDocument(MultipartFile file, Path targetPath) {
        Document doc = new Document();
        doc.setTitle(file.getOriginalFilename());
        doc.setFileType(file.getContentType());
        doc.setFilePath(targetPath.toAbsolutePath().toString());
        doc.setUploadDate(LocalDateTime.now());
        return documentRepository.save(doc);
    }

    private void sendToPythonService(Path targetPath, Document savedDoc) {
        try {
            validateFileExists(targetPath);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = createRequestEntity(targetPath);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    pythonServiceUrl + "/process-document",
                    requestEntity,
                    String.class
            );

            logPythonServiceResponse(response, savedDoc);

        } catch (Exception e) {
            logger.error("Error sending file to Python service (Document ID {}): {}",
                    savedDoc.getId(), e.getMessage(), e);
        }
    }

    private void validateFileExists(Path targetPath) throws IOException {
        if (!Files.exists(targetPath)) {
            logger.error("File does not exist at path: {}", targetPath);
            throw new IOException("Missing file for processing");
        }
    }

    private HttpEntity<MultiValueMap<String, Object>> createRequestEntity(Path targetPath) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        FileSystemResource fileResource = new FileSystemResource(targetPath.toFile());

        if (!fileResource.exists()) {
            logger.error("FileSystemResource could not find file: {}", targetPath);
            throw new IllegalArgumentException("File resource not found");
        }

        body.add("file", fileResource);
        return new HttpEntity<>(body, headers);
    }

    private void logPythonServiceResponse(ResponseEntity<String> response, Document savedDoc) {
        logger.info("Python service response for Document ID {}: Status={}, Body={}",
                savedDoc.getId(), response.getStatusCode(), response.getBody());
    }
}