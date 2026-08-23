package com.small.langchain.client.llm.guardrail;

import com.small.langchain.client.llm.guardrail.rules.NaoVazioGuardrail;
import com.small.langchain.client.llm.guardrail.rules.SemMarkdownGuardrail;
import com.small.langchain.client.llm.guardrail.rules.SemRecusaGuardrail;
import com.small.langchain.client.llm.guardrail.rules.TamanhoMinimoGuardrail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guardrails sao regras puras sobre texto, sem modelo no meio -- por isso da pra testar todos os
 * casos de borda em milissegundos, que e metade do motivo de te-los separados da chamada de IA.
 */
class GuardrailChainTest {

    private static final String PARECER_VALIDO = """
            Após análise da documentação apresentada pelo cliente, verificou-se que a movimentação
            é compatível com o perfil declarado. A consulta ao cadastro confirmou que a pessoa
            monitorada não se enquadra como PEP, PEM ou funcionária da instituição. Recomenda-se o
            encerramento do caso sem necessidade de comunicação ao COAF neste momento, mantendo a
            documentação arquivada para eventual reabertura.
            """;

    private final GuardrailChain cadeia = GuardrailChain.de(
            new NaoVazioGuardrail(),
            new SemRecusaGuardrail(),
            new TamanhoMinimoGuardrail(250),
            new SemMarkdownGuardrail());

    @Test
    @DisplayName("aprova sem ressalvas um parecer bem formado")
    void aprovaParecerValido() {
        GuardrailVeredito veredito = cadeia.validar(PARECER_VALIDO);

        assertThat(veredito.aprovado()).isTrue();
        assertThat(veredito.avisos()).isEmpty();
    }

    @Test
    @DisplayName("bloqueia resposta vazia, que é o sintoma de max_tokens estourado")
    void bloqueiaRespostaVazia() {
        GuardrailVeredito veredito = cadeia.validar("   ");

        assertThat(veredito.aprovado()).isFalse();
        assertThat(veredito.motivoBloqueio()).contains("nao-vazio");
    }

    @Test
    @DisplayName("bloqueia recusa do modelo, que chegaria como HTTP 200 e viraria parecer")
    void bloqueiaRecusa() {
        GuardrailVeredito veredito = cadeia.validar(
                "Desculpe, não posso ajudar com isso. Como modelo de linguagem, não tenho acesso a dados.");

        assertThat(veredito.aprovado()).isFalse();
        assertThat(veredito.motivoBloqueio()).contains("sem-recusa");
    }

    @Test
    @DisplayName("bloqueia texto curto demais, que indica resumo em vez de reescrita")
    void bloqueiaTextoCurto() {
        GuardrailVeredito veredito = cadeia.validar("Caso encerrado, sem indícios.");

        assertThat(veredito.aprovado()).isFalse();
        assertThat(veredito.motivoBloqueio()).contains("tamanho-minimo");
    }

    @Test
    @DisplayName("markdown apenas avisa: suja o relatório mas não invalida o conteúdo")
    void markdownGeraAvisoSemBloquear() {
        GuardrailVeredito veredito = cadeia.validar("## Parecer\n\n" + PARECER_VALIDO);

        assertThat(veredito.aprovado()).isTrue();
        assertThat(veredito.avisos()).hasSize(1);
        assertThat(veredito.avisos().getFirst()).contains("sem-markdown");
    }

    @Test
    @DisplayName("para no primeiro bloqueio, sem avaliar o resto da cadeia")
    void exigirLancaNoPrimeiroBloqueio() {
        assertThatThrownBy(() -> cadeia.exigir(""))
                .isInstanceOf(GuardrailViolationException.class)
                .hasMessageContaining("nao-vazio");
    }

    @Test
    @DisplayName("exigir devolve as ressalvas quando a saída passa com avisos")
    void exigirDevolveAvisos() {
        assertThat(cadeia.exigir("**Parecer**\n\n" + PARECER_VALIDO)).hasSize(1);
    }
}
