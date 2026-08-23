package com.small.langchain.client.llm.guardrail.rules;

import com.small.langchain.client.llm.guardrail.Guardrail;
import com.small.langchain.client.llm.guardrail.GuardrailResult;

/**
 * Resposta vazia e o sintoma mais comum de max_tokens estourado por modelo que "pensa" antes de
 * escrever: a geracao acaba dentro do raciocinio e nada sobra pro texto final.
 */
public class NaoVazioGuardrail implements Guardrail {

    @Override
    public String nome() {
        return "nao-vazio";
    }

    @Override
    public GuardrailResult validar(String saida) {
        return saida == null || saida.isBlank()
                ? GuardrailResult.reprovado("o modelo não retornou texto (possivelmente truncado por max_tokens)")
                : GuardrailResult.ok();
    }
}
