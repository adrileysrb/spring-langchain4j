package com.small.langchain.client;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

@Component
public class LlmClient {

    private OpenAiChatModel openAiChatModel = null;

    public OpenAiChatModel getInstance() {
        if(openAiChatModel != null) return openAiChatModel;

        openAiChatModel =  OpenAiChatModel.builder()
                .baseUrl("http://127.0.0.1:1234/v1")
                .apiKey("lm-studio")
                .modelName("google/gemma-4-e2b")
                .build();
        return openAiChatModel;
    }
}
