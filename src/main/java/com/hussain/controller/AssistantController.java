package com.hussain.controller;

import com.hussain.model.Prompt;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import com.hussain.service.AssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping(value = "/askFromAssistant")
    public ResponseEntity<String> askAssistant(@RequestBody Prompt prompt) {
        String response = assistantService.askAssistant(prompt);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
