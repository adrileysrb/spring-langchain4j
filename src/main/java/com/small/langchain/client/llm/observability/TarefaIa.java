package com.small.langchain.client.llm.observability;

/**
 * Catalogo das tarefas de IA instrumentadas. Concentrar os nomes num enum evita string solta
 * espalhada pelos servicos e mantem o agrupamento do endpoint de metricas estavel.
 */
public enum TarefaIa {

    /** Consulta do enquadramento PEP/PEM/funcionario via tools. */
    CONSULTA_ENQUADRAMENTO,

    /** Reescrita do parecer do analista. */
    REVISAO_PARECER,

    /** Classificacao de risco da ocorrencia com saida estruturada. */
    CLASSIFICACAO_RISCO,

    /** Extracao das pendencias citadas no parecer. */
    EXTRACAO_PENDENCIAS,

    /** Conversa do analista com o assistente, com memoria. */
    ASSISTENTE_ANALISTA,

    /** Pergunta respondida sobre a base de normativos (RAG). */
    CONSULTA_NORMATIVO,

    /** Chat livre do playground. */
    PLAYGROUND
}
