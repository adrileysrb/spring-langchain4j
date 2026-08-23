package com.small.langchain.client.llm.guardrail;

/** Resultado da avaliacao de um unico guardrail. */
public record GuardrailResult(boolean aprovado, String motivo) {

    private static final GuardrailResult OK = new GuardrailResult(true, null);

    public static GuardrailResult ok() {
        return OK;
    }

    public static GuardrailResult reprovado(String motivo) {
        return new GuardrailResult(false, motivo);
    }
}
