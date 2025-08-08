package com.hussain.service;

import com.hussain.model.Prompt;

public interface KnowledgeBaseService {
    String saveEmbeddings(Prompt prompt);

    String askExpertAssistant(Prompt prompt);
}
