package com.small.langchain.client.pldft.tool;

import com.small.langchain.client.pldft.enquadramento.Enquadramento;
import com.small.langchain.client.pldft.enquadramento.EnquadramentoLookup;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * Tool exposta a LLM: verifica se a pessoa monitorada de uma ocorrencia e PEP (Pessoa
 * Politicamente Exposta) -- informacao que nao aparece no texto do analista, apenas no cadastro.
 */
@Component
public class PepConsultaTool {

    private final EnquadramentoLookup lookup;

    PepConsultaTool(EnquadramentoLookup lookup) {
        this.lookup = lookup;
    }

    @Tool("Consulta no cadastro se a pessoa monitorada de uma ocorrencia de PLDFT e PEP (Pessoa " +
            "Politicamente Exposta). Use o id numerico da ocorrencia (o mesmo que aparece como " +
            "'Ocorrencia: #X' no contexto da analise). Retorna o enquadramento real, que deve ser " +
            "usado para resolver qualquer pendencia de verificacao de PEP mencionada no parecer.")
    public String consultarPep(@P("id da ocorrencia") long ocorrenciaId) {
        return lookup.porOcorrencia(ocorrenciaId)
                .map(Enquadramento::textoPep)
                .orElse("Não foi possível localizar a pessoa monitorada da ocorrência #" + ocorrenciaId + " no cadastro.");
    }
}
