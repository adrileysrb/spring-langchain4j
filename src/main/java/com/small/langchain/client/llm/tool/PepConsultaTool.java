package com.small.langchain.client.llm.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * Tool exposta a LLM durante a revisao do parecer de uma analise: verifica se a pessoa
 * monitorada de uma ocorrencia e PEP (Pessoa Politicamente Exposta) -- informacao que
 * nao aparece no texto bruto do analista, apenas no cadastro.
 */
@Component
public class PepConsultaTool {

    private final PessoaMonitoradaLookupService lookupService;

    PepConsultaTool(PessoaMonitoradaLookupService lookupService) {
        this.lookupService = lookupService;
    }

    @Tool("Consulta no cadastro se a pessoa monitorada de uma ocorrencia de PLDFT e PEP (Pessoa " +
            "Politicamente Exposta). Use o id numerico da ocorrencia (o mesmo que aparece como " +
            "'Ocorrencia: #X' no contexto da analise). Retorna o enquadramento real, que deve ser " +
            "usado para resolver qualquer pendencia de verificacao de PEP mencionada no parecer.")
    public String consultarPep(@P("id da ocorrencia") long ocorrenciaId) {
        return lookupService.porOcorrencia(ocorrenciaId)
                .map(pessoa -> "Pessoa monitorada: " + pessoa.nome()
                        + " | PEP (Pessoa Politicamente Exposta): " + PessoaMonitoradaLookupService.textoBooleano(pessoa.pep()))
                .orElse("Não foi possível localizar a pessoa monitorada da ocorrência #" + ocorrenciaId + " no cadastro.");
    }
}
