package com.small.langchain.client.pldft.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Feature flags do dominio PLDFT.
 *
 * <p>Separado de {@code LlmProperties} de proposito: la ficam os parametros do provedor de IA,
 * aqui ficam decisoes de como o negocio usa a IA. Sao ciclos de mudanca diferentes.
 */
@ConfigurationProperties(prefix = "pldft")
public record PldftProperties(

        /**
         * Quando {@code true}, o enquadramento (PEP/PEM/funcionario) e obtido fazendo o modelo
         * chamar uma tool por aspecto; quando {@code false} (padrao), e lido direto do cadastro.
         *
         * @see com.small.langchain.client.pldft.enquadramento.EnquadramentoStrategy
         */
        @DefaultValue("false") boolean enquadramentoViaTools
) {
}
