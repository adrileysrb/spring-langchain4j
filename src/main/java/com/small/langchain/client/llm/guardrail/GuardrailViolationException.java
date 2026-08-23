package com.small.langchain.client.llm.guardrail;

/** Lancada quando um guardrail bloqueante reprova a saida do modelo. */
public class GuardrailViolationException extends RuntimeException {

    private final String guardrail;

    public GuardrailViolationException(String guardrail, String motivo) {
        super("Saída reprovada pelo guardrail '" + guardrail + "': " + motivo);
        this.guardrail = guardrail;
    }

    public String guardrail() {
        return guardrail;
    }
}
