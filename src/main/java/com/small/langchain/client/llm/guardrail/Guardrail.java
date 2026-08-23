package com.small.langchain.client.llm.guardrail;

/**
 * Uma verificacao sobre o texto que o modelo produziu.
 *
 * <p>Cada implementacao checa exatamente uma coisa e nao conhece as outras -- e isso que permite
 * montar cadeias diferentes por fluxo e acrescentar regra nova sem tocar em nada existente.
 */
public interface Guardrail {

    String nome();

    GuardrailResult validar(String saida);

    default Severidade severidade() {
        return Severidade.BLOQUEIA;
    }
}
