package com.small.langchain.client.pldft.guardrail;

import com.small.langchain.client.llm.guardrail.Guardrail;
import com.small.langchain.client.llm.guardrail.GuardrailResult;

import java.util.List;
import java.util.Locale;

/**
 * Guardrail de dominio: o parecer reescrito precisa resolver a pendencia de enquadramento
 * (PEP, PEM ou vinculo de funcionario) que o rascunho do analista levantou.
 *
 * <p>Existe porque esse e o modo de falha mais caro deste fluxo: a tool consultou o cadastro, o
 * dado real entrou no prompt, e mesmo assim o modelo reescreveu o texto mantendo a verificacao
 * como "pendente". O resultado passaria por um parecer valido e estaria errado justamente no
 * ponto que motivou a automacao.
 *
 * <p>Regra generica nao pega isso -- por isso ela mora no pacote de dominio, e nao em llm/.
 */
public class MencionaEnquadramentoGuardrail implements Guardrail {

    private static final List<String> TERMOS = List.of("pep", "pem", "funcionári", "funcionari",
            "politicamente exposta", "exposta na mídia", "exposta na midia");

    @Override
    public String nome() {
        return "menciona-enquadramento";
    }

    @Override
    public GuardrailResult validar(String saida) {
        if (saida == null) {
            return GuardrailResult.ok();
        }
        String normalizado = saida.toLowerCase(Locale.ROOT);
        boolean mencionou = TERMOS.stream().anyMatch(normalizado::contains);
        return mencionou
                ? GuardrailResult.ok()
                : GuardrailResult.reprovado("o parecer revisado não menciona o enquadramento "
                        + "(PEP/PEM/funcionário), que foi consultado no cadastro e precisa constar");
    }
}
