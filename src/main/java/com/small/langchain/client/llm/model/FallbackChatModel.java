package com.small.langchain.client.llm.model;

import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.ChatRequestOptions;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator sobre {@link ChatModel}: tenta o modelo primario e, se ele falhar, repete a mesma
 * requisicao no modelo reserva. Como {@code ChatModel} e so uma interface, da pra empilhar
 * comportamento (fallback, cache, rate limit, auditoria) sem que quem chama perceba.
 *
 * <p>No caso deste projeto resolve um problema concreto: cada produto tem um modelo configurado
 * no banco, mas nem todos costumam estar carregados no LM Studio ao mesmo tempo.
 */
public class FallbackChatModel implements ChatModel {

    private static final Logger log = LoggerFactory.getLogger(FallbackChatModel.class);

    private final ChatModel primary;
    private final ChatModel fallback;
    private final String primaryName;

    public FallbackChatModel(ChatModel primary, ChatModel fallback, String primaryName) {
        this.primary = primary;
        this.fallback = fallback;
        this.primaryName = primaryName;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        try {
            return primary.chat(request);
        } catch (RuntimeException e) {
            log.warn("Modelo '{}' falhou ({}), repetindo a chamada no modelo padrão",
                    primaryName, e.getMessage());
            return fallback.chat(request);
        }
    }

    @Override
    public ChatResponse chat(ChatRequest request, ChatRequestOptions options) {
        try {
            return primary.chat(request, options);
        } catch (RuntimeException e) {
            log.warn("Modelo '{}' falhou ({}), repetindo a chamada no modelo padrão",
                    primaryName, e.getMessage());
            return fallback.chat(request, options);
        }
    }

    @Override
    public ChatRequestParameters defaultRequestParameters() {
        return primary.defaultRequestParameters();
    }

    @Override
    public ModelProvider provider() {
        return primary.provider();
    }
}
