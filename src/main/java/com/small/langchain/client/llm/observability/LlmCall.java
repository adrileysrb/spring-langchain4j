package com.small.langchain.client.llm.observability;

import java.time.LocalDateTime;

/** Registro de uma unica chamada ao modelo, capturado pelo listener. */
public record LlmCall(
        Long id,
        String tarefa,
        String modelo,
        Integer tokensEntrada,
        Integer tokensSaida,
        Integer tokensTotal,
        Long duracaoMs,
        String finishReason,
        boolean sucesso,
        String erro,
        LocalDateTime criadoEm
) {
}
