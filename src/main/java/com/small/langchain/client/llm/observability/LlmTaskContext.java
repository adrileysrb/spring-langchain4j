package com.small.langchain.client.llm.observability;

import java.util.function.Supplier;

/**
 * Marca qual tarefa de negocio esta rodando na thread atual (mesma ideia do MDC do slf4j).
 *
 * <p>O {@link dev.langchain4j.model.chat.listener.ChatModelListener} enxerga apenas a requisicao
 * tecnica -- mensagens, modelo, tokens -- e nao tem como saber se aquilo era uma revisao de parecer
 * ou uma classificacao de risco. Este contexto e a ponte: o servico declara a tarefa, o listener le.
 */
public final class LlmTaskContext {

    public static final String DESCONHECIDA = "DESCONHECIDA";

    private static final ThreadLocal<String> TAREFA_ATUAL = new ThreadLocal<>();

    private LlmTaskContext() {
    }

    public static <T> T executando(TarefaIa tarefa, Supplier<T> acao) {
        return executando(tarefa.name(), acao);
    }

    public static <T> T executando(String tarefa, Supplier<T> acao) {
        String anterior = TAREFA_ATUAL.get();
        TAREFA_ATUAL.set(tarefa);
        try {
            return acao.get();
        } finally {
            if (anterior == null) {
                TAREFA_ATUAL.remove();
            } else {
                TAREFA_ATUAL.set(anterior);
            }
        }
    }

    public static String atual() {
        String tarefa = TAREFA_ATUAL.get();
        return tarefa != null ? tarefa : DESCONHECIDA;
    }
}
