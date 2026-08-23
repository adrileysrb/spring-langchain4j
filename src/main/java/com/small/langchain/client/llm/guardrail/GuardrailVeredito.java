package com.small.langchain.client.llm.guardrail;

import java.util.List;

/** Resultado consolidado da cadeia: o que bloqueou (se bloqueou) e as ressalvas acumuladas. */
public record GuardrailVeredito(boolean aprovado, String motivoBloqueio, List<String> avisos) {

    public boolean temAvisos() {
        return !avisos.isEmpty();
    }
}
