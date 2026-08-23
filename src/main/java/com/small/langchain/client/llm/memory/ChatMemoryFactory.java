package com.small.langchain.client.llm.memory;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.stereotype.Component;

/**
 * Cria a memoria de uma conversa. {@link MessageWindowChatMemory} mantem apenas as N ultimas
 * mensagens: a janela de contexto do modelo e finita e, num modelo local pequeno, ela acaba bem
 * antes do que se imagina -- sem esse corte, a conversa simplesmente para de funcionar depois
 * de algumas trocas.
 *
 * <p>{@code alwaysKeepSystemMessageFirst} garante que a instrucao de papel nao seja descartada
 * junto com as mensagens antigas; sem isso o assistente "esquece" quem e no meio da conversa.
 */
@Component
public class ChatMemoryFactory {

    private final ChatMemoryStore chatMemoryStore;

    public ChatMemoryFactory(ChatMemoryStore chatMemoryStore) {
        this.chatMemoryStore = chatMemoryStore;
    }

    public ChatMemory paraConversa(String conversaId, int maxMensagens) {
        return MessageWindowChatMemory.builder()
                .id(conversaId)
                .maxMessages(maxMensagens)
                .alwaysKeepSystemMessageFirst(true)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }
}
