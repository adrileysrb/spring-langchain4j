package com.small.langchain.client.llm.guardrail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Chain of Responsibility sobre a saida do modelo: cada {@link Guardrail} recebe o texto na sua
 * vez; o primeiro bloqueante que reprovar encerra a cadeia, e os que apenas avisam acumulam
 * ressalvas sem interromper.
 *
 * <p>A cadeia e montada por fluxo, nao globalmente -- a reescrita de parecer e a resposta do
 * assistente tem exigencias diferentes sobre o mesmo modelo.
 */
public final class GuardrailChain {

    private static final Logger log = LoggerFactory.getLogger(GuardrailChain.class);

    private final List<Guardrail> guardrails;

    private GuardrailChain(List<Guardrail> guardrails) {
        this.guardrails = guardrails;
    }

    public static GuardrailChain de(Guardrail... guardrails) {
        return new GuardrailChain(List.of(guardrails));
    }

    public GuardrailVeredito validar(String saida) {
        List<String> avisos = new ArrayList<>();

        for (Guardrail guardrail : guardrails) {
            GuardrailResult resultado = guardrail.validar(saida);
            if (resultado.aprovado()) {
                continue;
            }
            if (guardrail.severidade() == Severidade.BLOQUEIA) {
                return new GuardrailVeredito(false, guardrail.nome() + ": " + resultado.motivo(), avisos);
            }
            avisos.add(guardrail.nome() + ": " + resultado.motivo());
        }
        return new GuardrailVeredito(true, null, avisos);
    }

    /**
     * Valida e lanca se houver bloqueio. Devolve as ressalvas para quem quiser registra-las
     * junto do resultado.
     */
    public List<String> exigir(String saida) {
        GuardrailVeredito veredito = validar(saida);
        if (!veredito.aprovado()) {
            throw new GuardrailViolationException("cadeia", veredito.motivoBloqueio());
        }
        if (veredito.temAvisos()) {
            log.warn("Saída aprovada com ressalvas: {}", String.join("; ", veredito.avisos()));
        }
        return veredito.avisos();
    }
}
