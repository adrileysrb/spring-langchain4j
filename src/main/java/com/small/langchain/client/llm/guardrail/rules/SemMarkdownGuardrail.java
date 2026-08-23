package com.small.langchain.client.llm.guardrail.rules;

import com.small.langchain.client.llm.guardrail.Guardrail;
import com.small.langchain.client.llm.guardrail.GuardrailResult;
import com.small.langchain.client.llm.guardrail.Severidade;

import java.util.regex.Pattern;

/**
 * Verifica se o modelo respeitou o "sem markdown" do prompt. Fica em {@link Severidade#AVISA}
 * de proposito: formatacao indevida suja o relatorio mas nao invalida o conteudo, e derrubar a
 * geracao inteira por causa de um asterisco seria pior que registrar a ressalva.
 */
public class SemMarkdownGuardrail implements Guardrail {

    private static final Pattern MARCACAO = Pattern.compile("```|\\*\\*|^#{1,6}\\s", Pattern.MULTILINE);

    @Override
    public String nome() {
        return "sem-markdown";
    }

    @Override
    public Severidade severidade() {
        return Severidade.AVISA;
    }

    @Override
    public GuardrailResult validar(String saida) {
        return saida != null && MARCACAO.matcher(saida).find()
                ? GuardrailResult.reprovado("a resposta contém marcação markdown, que o prompt pedia para evitar")
                : GuardrailResult.ok();
    }
}
