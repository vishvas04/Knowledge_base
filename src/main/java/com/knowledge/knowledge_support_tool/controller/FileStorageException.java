package com.knowledge.knowledge_support_tool.controller;

public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message); // Fix constructor
    }
}