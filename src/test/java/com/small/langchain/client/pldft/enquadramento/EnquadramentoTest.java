package com.small.langchain.client.pldft.enquadramento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A redacao do enquadramento vai direto pro prompt, e as duas abordagens
 * ({@link EnquadramentoStrategy}) precisam produzir o mesmo vocabulario -- senao a diferenca de
 * resultado entre elas viria do texto, nao da abordagem.
 */
class EnquadramentoTest {

    private final Enquadramento ana = new Enquadramento("Ana Lima", true, false, false);

    @Test
    @DisplayName("linha única traz os três aspectos, usada pela consulta direta")
    void textoCompletoTrazOsTresAspectos() {
        assertThat(ana.textoCompleto())
                .contains("Ana Lima")
                .contains("PEP (Pessoa Politicamente Exposta): sim")
                .contains("PEM (Pessoa Exposta na Mídia): não")
                .contains("Funcionário da instituição: não");
    }

    @Test
    @DisplayName("cada texto por aspecto é auto-contido, porque cada tool responde sozinha")
    void textosPorAspectoSaoAutoContidos() {
        assertThat(ana.textoPep()).contains("Ana Lima").contains("PEP (Pessoa Politicamente Exposta): sim");
        assertThat(ana.textoPem()).contains("Ana Lima").contains("PEM (Pessoa Exposta na Mídia): não");
        assertThat(ana.textoFuncionario()).contains("Ana Lima").contains("Funcionário da instituição: não");
    }

    @Test
    @DisplayName("as duas abordagens usam o mesmo vocabulário para o mesmo dado")
    void abordagensUsamOMesmoVocabulario() {
        Enquadramento pessoa = new Enquadramento("Fernanda Costa", false, true, false);

        // O texto via tools é a concatenação dos três resultados; o direto é uma linha só.
        // O formato difere, os termos não -- e é o termo que o guardrail e o modelo enxergam.
        String viaTools = String.join("\n", pessoa.textoPep(), pessoa.textoPem(), pessoa.textoFuncionario());

        assertThat(viaTools).contains("PEM (Pessoa Exposta na Mídia): sim");
        assertThat(pessoa.textoCompleto()).contains("PEM (Pessoa Exposta na Mídia): sim");
    }
}
