package com.small.langchain.client.llm.guardrail.rules;

import com.small.langchain.client.llm.guardrail.Guardrail;
import com.small.langchain.client.llm.guardrail.GuardrailResult;

import java.util.List;
import java.util.Locale;

/**
 * Detecta o modelo recusando a tarefa ou saindo do papel em vez de produzir o texto pedido.
 *
 * <p>Importa porque a recusa chega como HTTP 200 com texto plausivel: sem essa checagem,
 * "não posso ajudar com isso" seria gravado no banco como se fosse o parecer revisado.
 */
public class SemRecusaGuardrail implements Guardrail {

    private static final List<String> MARCADORES_DE_RECUSA = List.of(
            "não posso ajudar",
            "nao posso ajudar",
            "não posso atender",
            "não sou capaz de",
            "como modelo de linguagem",
            "como uma inteligência artificial",
            "sou apenas uma ia",
            "i cannot",
            "i'm sorry, but",
            "as an ai"
    );

    @Override
    public String nome() {
        return "sem-recusa";
    }

    @Override
    public GuardrailResult validar(String saida) {
        if (saida == null) {
            return GuardrailResult.ok();
        }
        String normalizado = saida.toLowerCase(Locale.ROOT);
        return MARCADORES_DE_RECUSA.stream()
                .filter(normalizado::contains)
                .findFirst()
                .map(marcador -> GuardrailResult.reprovado(
                        "o modelo recusou a tarefa ou saiu do papel (\"" + marcador + "\")"))
                .orElseGet(GuardrailResult::ok);
    }
}
