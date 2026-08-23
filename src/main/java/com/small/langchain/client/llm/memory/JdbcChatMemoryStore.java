package com.small.langchain.client.llm.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter entre o {@link ChatMemoryStore} do langchain4j e uma tabela relacional.
 *
 * <p>A implementacao que vem pronta ({@code InMemoryChatMemoryStore}) perde tudo ao reiniciar a
 * aplicacao -- serve pra exemplo, nao pra um caso em que a conversa e evidencia de auditoria.
 * Trocar a persistencia da memoria e exatamente para isso que a interface existe.
 *
 * <p>Uma linha por mensagem, e nao um JSON unico por conversa, porque assim a conversa fica
 * legivel e consultavel em SQL. Em troca, {@code updateMessages} regrava a conversa inteira: a
 * interface entrega sempre a lista completa e inferir o delta seria adivinhacao -- barato aqui,
 * onde a janela e de poucas mensagens.
 *
 * <p>A serializacao usa {@link ChatMessageSerializer}, que preserva o tipo da mensagem
 * (sistema, usuario, IA, resultado de tool) e os tool calls -- informacao que se perderia se a
 * gravacao fosse so o texto.
 */
@Component
public class JdbcChatMemoryStore implements ChatMemoryStore {

    private final JdbcTemplate jdbcTemplate;

    public JdbcChatMemoryStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        return jdbcTemplate.query(
                "SELECT mensagem_json FROM conversa_mensagens WHERE conversa_id = ? ORDER BY ordem",
                (rs, rowNum) -> ChatMessageDeserializer.messageFromJson(rs.getString("mensagem_json")),
                id(memoryId));
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String conversaId = id(memoryId);
        deleteMessages(memoryId);

        List<Object[]> parametros = new java.util.ArrayList<>(messages.size());
        for (int ordem = 0; ordem < messages.size(); ordem++) {
            parametros.add(new Object[]{
                    conversaId, ordem, ChatMessageSerializer.messageToJson(messages.get(ordem))});
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO conversa_mensagens (conversa_id, ordem, mensagem_json) VALUES (?, ?, ?)",
                parametros);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        jdbcTemplate.update("DELETE FROM conversa_mensagens WHERE conversa_id = ?", id(memoryId));
    }

    private String id(Object memoryId) {
        return String.valueOf(memoryId);
    }
}
