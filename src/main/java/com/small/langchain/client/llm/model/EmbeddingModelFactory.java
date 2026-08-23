package com.small.langchain.client.llm.model;

import com.small.langchain.client.llm.config.LlmProperties;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.stereotype.Component;

/**
 * Fabrica preguicosa do {@link EmbeddingModel} usado pelo RAG. A construcao so acontece no
 * primeiro uso porque o modelo de embeddings e opcional: a aplicacao sobe normalmente mesmo
 * sem provedor rodando, e so quem chama a busca semantica sente a falta.
 */
@Component
public class EmbeddingModelFactory {

    private final LlmProperties properties;
    private volatile EmbeddingModel embeddingModel;

    public EmbeddingModelFactory(LlmProperties properties) {
        this.properties = properties;
    }

    public EmbeddingModel getInstance() {
        EmbeddingModel local = embeddingModel;
        if (local == null) {
            synchronized (this) {
                local = embeddingModel;
                if (local == null) {
                    local = OpenAiEmbeddingModel.builder()
                            .baseUrl(properties.baseUrl())
                            .apiKey(properties.apiKey())
                            .modelName(properties.embeddingModel())
                            .timeout(properties.timeout())
                            .maxRetries(properties.maxRetries())
                            .build();
                    embeddingModel = local;
                }
            }
        }
        return local;
    }
}
