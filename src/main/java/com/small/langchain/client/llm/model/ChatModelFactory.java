package com.small.langchain.client.llm.model;

import com.small.langchain.client.llm.config.LlmProperties;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ponto unico de criacao de {@link ChatModel}. Alem de centralizar a configuracao, guarda uma
 * instancia por {@link ModelSpec} -- construir o modelo e barato, mas cada instancia carrega seu
 * proprio HTTP client, entao reaproveitar evita criar um pool novo a cada requisicao.
 *
 * <p>Todo {@link ChatModelListener} registrado como bean e plugado automaticamente nos modelos
 * criados aqui: e assim que a observabilidade entra sem nenhum servico saber que ela existe.
 */
@Component
public class ChatModelFactory {

    private final LlmProperties properties;
    private final List<ChatModelListener> listeners;
    private final Map<ModelSpec, ChatModel> cache = new ConcurrentHashMap<>();

    public ChatModelFactory(LlmProperties properties, List<ChatModelListener> listeners) {
        this.properties = properties;
        this.listeners = listeners;
    }

    public ChatModel defaultModel() {
        return forSpec(properties.defaultSpec());
    }

    public ChatModel forSpec(ModelSpec spec) {
        ModelSpec normalized = spec.withDefaults(
                properties.defaultModel(), properties.defaultTemperature(), properties.defaultMaxTokens());

        // computeIfAbsent nao serve aqui: create() pode chamar forSpec() de novo pra montar o
        // fallback, e ConcurrentHashMap proibe atualizacao recursiva.
        ChatModel cached = cache.get(normalized);
        if (cached != null) {
            return cached;
        }
        ChatModel created = create(normalized);
        ChatModel raced = cache.putIfAbsent(normalized, created);
        return raced != null ? raced : created;
    }

    private ChatModel create(ModelSpec spec) {
        ChatModel model = openAiModel(spec);
        boolean isDefault = spec.modelName().equals(properties.defaultModel());
        if (isDefault || !properties.fallbackToDefaultModel()) {
            return model;
        }
        return new FallbackChatModel(model, defaultModel(), spec.modelName());
    }

    private ChatModel openAiModel(ModelSpec spec) {
        return OpenAiChatModel.builder()
                .baseUrl(properties.baseUrl())
                .apiKey(properties.apiKey())
                .modelName(spec.modelName())
                .temperature(spec.temperature())
                .maxTokens(spec.maxTokens())
                .frequencyPenalty(properties.frequencyPenalty())
                .presencePenalty(properties.presencePenalty())
                .timeout(properties.timeout())
                .maxRetries(properties.maxRetries())
                .listeners(listeners)
                .build();
    }
}
