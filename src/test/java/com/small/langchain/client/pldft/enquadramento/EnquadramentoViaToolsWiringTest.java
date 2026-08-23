package com.small.langchain.client.pldft.enquadramento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O teste de condicao isolado prova que a flag escolhe o bean certo, mas nao que a aplicacao
 * inteira sobe nessa configuracao -- a abordagem via tools puxa dependencias que a direta nao
 * usa. Sem isto, ligar a flag em producao poderia quebrar no start, e a configuracao alternativa
 * viraria caminho morto que ninguem exercita.
 */
@SpringBootTest(properties = "pldft.enquadramento-via-tools=true")
class EnquadramentoViaToolsWiringTest {

    @Autowired
    private EnquadramentoStrategy strategy;

    @Test
    @DisplayName("a aplicação inteira sobe com a flag ligada, usando a abordagem via tools")
    void contextoSobeComAFlagLigada() {
        assertThat(strategy).isInstanceOf(EnquadramentoViaTools.class);
        assertThat(strategy.nome()).isEqualTo("via-tools");
    }
}
