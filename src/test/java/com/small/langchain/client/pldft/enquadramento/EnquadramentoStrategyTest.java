package com.small.langchain.client.pldft.enquadramento;

import com.small.langchain.client.llm.tool.ToolLoopRunner;
import com.small.langchain.client.pldft.model.Ocorrencia;
import com.small.langchain.client.pldft.tool.OcorrenciaTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnquadramentoStrategyTest {

    private static final Ocorrencia OCORRENCIA =
            new Ocorrencia(3L, 3L, 1L, "ABERTA", LocalDateTime.now(), null);

    @Test
    @DisplayName("a abordagem direta funciona sem modelo nenhum -- nem null a incomoda")
    void diretoNaoUsaOModelo() {
        EnquadramentoLookup lookup = mock(EnquadramentoLookup.class);
        when(lookup.porOcorrencia(3L))
                .thenReturn(Optional.of(new Enquadramento("Ana Lima", true, false, false)));

        // Passar null como ChatModel é a asserção de verdade aqui: se a implementação tocasse no
        // modelo, isso explodiria. Zero token, zero latência de inferência.
        String texto = new EnquadramentoDireto(lookup).consultar(OCORRENCIA, null);

        assertThat(texto).contains("Ana Lima").contains("PEP (Pessoa Politicamente Exposta): sim");
    }

    @Test
    @DisplayName("pessoa fora do cadastro devolve null, e o prompt segue avisando disso")
    void diretoDevolveNullQuandoNaoEncontra() {
        EnquadramentoLookup lookup = mock(EnquadramentoLookup.class);
        when(lookup.porOcorrencia(3L)).thenReturn(Optional.empty());

        assertThat(new EnquadramentoDireto(lookup).consultar(OCORRENCIA, null)).isNull();
    }

    // -- A flag de fato troca a implementação? Vale verificar: condição errada falha em silêncio,
    // -- criando o bean que não devia e sem nenhum erro visível.

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(EnquadramentoLookup.class, () -> mock(EnquadramentoLookup.class))
            .withBean(ToolLoopRunner.class, () -> mock(ToolLoopRunner.class))
            .withBean(OcorrenciaTools.class, () -> mock(OcorrenciaTools.class))
            .withUserConfiguration(EnquadramentoDireto.class, EnquadramentoViaTools.class);

    @Test
    @DisplayName("sem a flag, vale a abordagem direta")
    void padraoEhDireto() {
        runner.run(context -> assertThat(context.getBean(EnquadramentoStrategy.class))
                .isInstanceOf(EnquadramentoDireto.class));
    }

    @Test
    @DisplayName("flag explicitamente false mantém a abordagem direta")
    void flagFalsaMantemDireto() {
        runner.withPropertyValues("pldft.enquadramento-via-tools=false")
                .run(context -> assertThat(context.getBean(EnquadramentoStrategy.class))
                        .isInstanceOf(EnquadramentoDireto.class));
    }

    @Test
    @DisplayName("flag ligada troca para a abordagem via tools")
    void flagLigadaUsaTools() {
        runner.withPropertyValues("pldft.enquadramento-via-tools=true")
                .run(context -> assertThat(context.getBean(EnquadramentoStrategy.class))
                        .isInstanceOf(EnquadramentoViaTools.class));
    }

    @Test
    @DisplayName("nunca existem as duas ao mesmo tempo, senão a injeção ficaria ambígua")
    void apenasUmaEstrategiaPorVez() {
        runner.run(context -> assertThat(context.getBeansOfType(EnquadramentoStrategy.class)).hasSize(1));
        runner.withPropertyValues("pldft.enquadramento-via-tools=true")
                .run(context -> assertThat(context.getBeansOfType(EnquadramentoStrategy.class)).hasSize(1));
    }
}
