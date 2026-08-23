package com.small.langchain.client.llm.guardrail.rules;

import com.small.langchain.client.llm.guardrail.Guardrail;
import com.small.langchain.client.llm.guardrail.GuardrailResult;

/**
 * Barra resposta curta demais pro que foi pedido. Um parecer reescrito que volta com duas linhas
 * quase sempre significa que o modelo resumiu em vez de reescrever -- e resumo perde justamente
 * as pendencias e decisoes que precisavam ser preservadas.
 */
public class TamanhoMinimoGuardrail implements Guardrail {

    private final int minimoDeCaracteres;

    public TamanhoMinimoGuardrail(int minimoDeCaracteres) {
        this.minimoDeCaracteres = minimoDeCaracteres;
    }

    @Override
    public String nome() {
        return "tamanho-minimo";
    }

    @Override
    public GuardrailResult validar(String saida) {
        int tamanho = saida != null ? saida.strip().length() : 0;
        return tamanho < minimoDeCaracteres
                ? GuardrailResult.reprovado("texto com " + tamanho + " caracteres, abaixo do mínimo de "
                        + minimoDeCaracteres + " esperado para este tipo de resposta")
                : GuardrailResult.ok();
    }
}
