package com.small.langchain.client.llm.guardrail;

/**
 * Nem toda violacao merece o mesmo tratamento: resposta vazia inutiliza o resultado, enquanto
 * markdown onde se pediu texto puro e apenas feio. Separar os dois evita o pior dos mundos --
 * ou barrar demais e derrubar o fluxo, ou logar tudo e deixar passar saida quebrada.
 */
public enum Severidade {

    /** Interrompe a cadeia e invalida a saida. */
    BLOQUEIA,

    /** Registra a ressalva e deixa a saida seguir. */
    AVISA
}
