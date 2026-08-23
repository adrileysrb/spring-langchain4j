package com.small.langchain.client.llm.model;

import com.small.langchain.client.llm.config.LlmProperties;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mesma ideia do {@link ChatModelFactory}, mas para {@link StreamingChatModel}. O langchain4j
 * separa os dois porque o contrato muda: em vez de devolver a resposta pronta, o modelo entrega
 * pedacos via callback, o que exige um caminho diferente ate o cliente HTTP (SSE, WebSocket...).
 */
@Component
public class StreamingChatModelFactory {

    private final LlmProperties properties;
    private final List<ChatModelListener> listeners;
    private final Map<ModelSpec, StreamingChatModel> cache = new ConcurrentHashMap<>();

    public StreamingChatModelFactory(LlmProperties properties, List<ChatModelListener> listeners) {
        this.properties = properties;
        this.listeners = listeners;
    }

    public StreamingChatModel defaultModel() {
        return forSpec(properties.defaultSpec());
    }

    public StreamingChatModel forSpec(ModelSpec spec) {
        ModelSpec normalized = spec.withDefaults(
                properties.defaultModel(), properties.defaultTemperature(), properties.defaultMaxTokens());
        return cache.computeIfAbsent(normalized, this::create);
    }

    private StreamingChatModel create(ModelSpec spec) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(properties.baseUrl())
                .apiKey(properties.apiKey())
                .modelName(spec.modelName())
                .temperature(spec.temperature())
                .maxTokens(spec.maxTokens())
                .frequencyPenalty(properties.frequencyPenalty())
                .presencePenalty(properties.presencePenalty())
                .timeout(properties.timeout())
                .listeners(listeners)
                .build();
    }
}
