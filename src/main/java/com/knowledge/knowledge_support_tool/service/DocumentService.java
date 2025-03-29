package com.knowledge.knowledge_support_tool.service;

import com.knowledge.knowledge_support_tool.model.Document;
import com.knowledge.knowledge_support_tool.repository.DocumentRepository;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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
        logger.info("Received file: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());

        try {
            if (file.isEmpty() || file.getSize() == 0) {
                logger.warn("Attempted to upload an empty file.");
                throw new IOException("Upload failed: The file is empty.");
            }

            Path uploadDir = Paths.get(storagePath);
            if (!Files.exists(uploadDir)) {
                logger.info("Creating upload directory: {}", uploadDir);
                Files.createDirectories(uploadDir);
            }

            String originalName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            logger.info("Original filename cleaned: {}", originalName);

            String fileExtension = FilenameUtils.getExtension(originalName);
            String baseName = FilenameUtils.getBaseName(originalName);
            String safeFileName = baseName.replaceAll("[^\\w.-]", "_")
                    + "_" + UUID.randomUUID().toString().substring(0, 8)
                    + "." + fileExtension;

            Path targetPath = uploadDir.resolve(safeFileName);
            logger.info("Storing file at: {}", targetPath);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            if (!Files.exists(targetPath) || !Files.isReadable(targetPath)) {
                logger.error("File not found or not readable: {}", targetPath);
                throw new IOException("File was not saved properly.");
            }

            logger.info("File saved successfully at: {}", targetPath);

            Document doc = new Document();
            doc.setTitle(originalName);
            doc.setFileType(file.getContentType());
            doc.setFilePath(targetPath.toAbsolutePath().toString());
            doc.setUploadDate(LocalDateTime.now());

            Document savedDoc = documentRepository.save(doc);
            logger.info("Document saved with ID: {}", savedDoc.getId());

            sendToPythonService(targetPath);
            return savedDoc;

        } catch (Exception e) {
            logger.info("In catch block");
            logger.error("Document processing failed: {}", e.getMessage(), e);
            throw new IOException("Failed to process document: " + e.getMessage(), e);
        }
    }

    private void sendToPythonService(Path targetPath) {
        try {
            if (!Files.exists(targetPath)) {
                logger.info("In Catch block");
                logger.error("File does not exist at path: {}", targetPath);
                return;
            }

            File file = targetPath.toFile();
            logger.info("📂 File exists: {}, Size: {} bytes", file.getAbsolutePath(), file.length());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            FileSystemResource fileResource = new FileSystemResource(targetPath.toFile());

            if (!fileResource.exists()) {
                logger.error("FileSystemResource could not find file: {}", targetPath);
                return;
            }

            body.add("file", fileResource);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    pythonServiceUrl + "/process-document",
                    requestEntity,
                    String.class
            );

            logger.info("Response from Python service: Status={}, Body={}", response.getStatusCode(), response.getBody());
        } catch (Exception e) {
            logger.error("Error sending file to Python service: {}", e.getMessage(), e);
        }
    }
}