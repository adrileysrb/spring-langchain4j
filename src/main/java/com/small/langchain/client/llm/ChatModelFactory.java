package com.small.langchain.client.llm;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ChatModelFactory {

    private static final String BASE_URL = "http://127.0.0.1:1234/v1";
    private static final String API_KEY = "lm-studio";
    private static final String DEFAULT_MODEL_NAME = "google/gemma-4-e2b";

    // Modelos locais "raciocinam" antes de responder e podem consumir boa parte
    // do max_tokens so com isso, o que deixa a geracao bem mais lenta que uma
    // API hospedada -- por isso o timeout generoso e poucos retries.
    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    private OpenAiChatModel defaultChatModel;

    public synchronized OpenAiChatModel defaultChatModel() {
        if (defaultChatModel == null) {
            defaultChatModel = OpenAiChatModel.builder()
                    .baseUrl(BASE_URL)
                    .apiKey(API_KEY)
                    .modelName(DEFAULT_MODEL_NAME)
                    .timeout(TIMEOUT)
                    .maxRetries(1)
                    .build();
        }
        return defaultChatModel;
    }

    public OpenAiChatModel build(String modelName, Double temperature, Integer maxTokens) {
        return OpenAiChatModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(API_KEY)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                // modelos locais pequenos tendem a entrar em loop repetindo o mesmo trecho
                // ate estourar o max_tokens; penalizar repeticao reduz bastante isso.
                .frequencyPenalty(0.6)
                .presencePenalty(0.4)
                .timeout(TIMEOUT)
                .maxRetries(1)
                .build();
    }
}
