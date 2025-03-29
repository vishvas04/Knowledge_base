package com.knowledge.knowledge_support_tool.controller;

import com.knowledge.knowledge_support_tool.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    @Autowired
    private QueryService queryService;

    @PostMapping
    public ResponseEntity<String> processQuery(@RequestBody Map<String, String> request) {
        String response = queryService.processQuery(request.get("question"));
        return ResponseEntity.ok(response);
    }
}
