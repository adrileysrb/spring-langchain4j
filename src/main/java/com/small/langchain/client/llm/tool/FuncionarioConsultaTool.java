package com.small.langchain.client.llm.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * Tool exposta a LLM durante a revisao do parecer de uma analise: verifica se a pessoa
 * monitorada de uma ocorrencia e funcionaria da instituicao financeira -- informacao
 * que nao aparece no texto bruto do analista, apenas no cadastro.
 */
@Component
public class FuncionarioConsultaTool {

    private final PessoaMonitoradaLookupService lookupService;

    FuncionarioConsultaTool(PessoaMonitoradaLookupService lookupService) {
        this.lookupService = lookupService;
    }

    @Tool("Consulta no cadastro se a pessoa monitorada de uma ocorrencia de PLDFT e funcionaria da " +
            "instituicao financeira. Use o id numerico da ocorrencia (o mesmo que aparece como " +
            "'Ocorrencia: #X' no contexto da analise). Retorna o enquadramento real, que deve ser " +
            "usado para resolver qualquer pendencia de verificacao de vinculo com a instituicao " +
            "mencionada no parecer.")
    public String consultarFuncionario(@P("id da ocorrencia") long ocorrenciaId) {
        return lookupService.porOcorrencia(ocorrenciaId)
                .map(pessoa -> "Pessoa monitorada: " + pessoa.nome()
                        + " | Funcionário da instituição: " + PessoaMonitoradaLookupService.textoBooleano(pessoa.funcionario()))
                .orElse("Não foi possível localizar a pessoa monitorada da ocorrência #" + ocorrenciaId + " no cadastro.");
    }
}
