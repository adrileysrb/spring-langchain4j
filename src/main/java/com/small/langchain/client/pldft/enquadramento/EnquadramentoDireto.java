package com.small.langchain.client.pldft.enquadramento;

import com.small.langchain.client.pldft.model.Ocorrencia;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Abordagem padrao: le o enquadramento direto do cadastro, sem passar pelo modelo.
 *
 * <p>Uma consulta, nenhum token, nenhuma latencia de inferencia e nenhuma chance de o modelo
 * responder por conta propria. O dado e obrigatorio e a query e conhecida -- nao existe decisao
 * para delegar.
 *
 * <p>Ativa quando {@code pldft.enquadramento-via-tools} e {@code false} ou nao esta definida.
 */
@Component
@ConditionalOnProperty(name = "pldft.enquadramento-via-tools", havingValue = "false", matchIfMissing = true)
public class EnquadramentoDireto implements EnquadramentoStrategy {

    private static final Logger log = LoggerFactory.getLogger(EnquadramentoDireto.class);

    private final EnquadramentoLookup lookup;

    EnquadramentoDireto(EnquadramentoLookup lookup) {
        this.lookup = lookup;
    }

    @Override
    public String nome() {
        return "direto";
    }

    @Override
    public String consultar(Ocorrencia ocorrencia, ChatModel chatModel) {
        return lookup.porOcorrencia(ocorrencia.id())
                .map(Enquadramento::textoCompleto)
                .orElseGet(() -> {
                    log.warn("Ocorrência {}: pessoa monitorada não encontrada no cadastro", ocorrencia.id());
                    return null;
                });
    }
}
