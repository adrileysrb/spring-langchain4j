package com.small.langchain.client.llm.observability;

/** Consolidado por tarefa: quanto cada funcionalidade custa em tokens e tempo. */
public record LlmCallStats(
        String tarefa,
        long chamadas,
        long erros,
        long tokensEntrada,
        long tokensSaida,
        long tokensTotal,
        Long duracaoMediaMs
) {
}
