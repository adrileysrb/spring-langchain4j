package com.small.langchain.client.llm.tool;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Saida do loop de tools. Traz as tres coisas que os diferentes casos de uso precisam:
 * a conversa completa (pra alimentar memoria), os resultados crus das tools (pra injetar como
 * contexto em outra chamada) e a ultima mensagem do modelo (pra devolver ao usuario).
 */
public record ToolLoopResult(
        List<ChatMessage> mensagens,
        List<ToolExecutionResultMessage> resultadosDeTools,
        AiMessage respostaFinal
) {

    public boolean algumaToolExecutada() {
        return !resultadosDeTools.isEmpty();
    }

    /** Texto da ultima resposta do modelo, ou {@code null} se ele so emitiu tool calls. */
    public String texto() {
        return respostaFinal != null ? respostaFinal.text() : null;
    }

    /** Resultados das tools concatenados, prontos pra virar contexto de um proximo prompt. */
    public String resultadosConcatenados(String separador) {
        return resultadosDeTools.stream()
                .map(ToolExecutionResultMessage::text)
                .collect(Collectors.joining(separador));
    }
}
