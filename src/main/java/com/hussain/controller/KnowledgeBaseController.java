package com.hussain.controller;

import com.hussain.model.Prompt;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.hussain.service.KnowledgeBaseService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @PostMapping(value = "/public/save-embeddings")
    public ResponseEntity<String> saveEmbeddings(@RequestBody Prompt prompt) {
        String response = knowledgeBaseService.saveEmbeddings(prompt);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(value = "/askFromExpertAssistant")
    public ResponseEntity<String> askExpertAssistant(@RequestBody Prompt prompt) {
        String response = knowledgeBaseService.askExpertAssistant(prompt);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
