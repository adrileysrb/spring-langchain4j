package com.small.langchain.client.pldft.tool;

import com.small.langchain.client.pldft.enquadramento.Enquadramento;
import com.small.langchain.client.pldft.enquadramento.EnquadramentoLookup;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * Tool exposta a LLM: verifica se a pessoa monitorada de uma ocorrencia e PEM (Pessoa Exposta
 * na Midia) -- informacao que nao aparece no texto do analista, apenas no cadastro.
 */
@Component
public class PemConsultaTool {

    private final EnquadramentoLookup lookup;

    PemConsultaTool(EnquadramentoLookup lookup) {
        this.lookup = lookup;
    }

    @Tool("Consulta no cadastro se a pessoa monitorada de uma ocorrencia de PLDFT e PEM (Pessoa " +
            "Exposta na Midia). Use o id numerico da ocorrencia (o mesmo que aparece como " +
            "'Ocorrencia: #X' no contexto da analise). Retorna o enquadramento real, que deve ser " +
            "usado para resolver qualquer pendencia de verificacao de PEM mencionada no parecer.")
    public String consultarPem(@P("id da ocorrencia") long ocorrenciaId) {
        return lookup.porOcorrencia(ocorrenciaId)
                .map(Enquadramento::textoPem)
                .orElse("Não foi possível localizar a pessoa monitorada da ocorrência #" + ocorrenciaId + " no cadastro.");
    }
}
