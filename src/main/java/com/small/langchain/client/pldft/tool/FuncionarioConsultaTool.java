package com.small.langchain.client.pldft.tool;

import com.small.langchain.client.pldft.enquadramento.Enquadramento;
import com.small.langchain.client.pldft.enquadramento.EnquadramentoLookup;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * Tool exposta a LLM: verifica se a pessoa monitorada de uma ocorrencia e funcionaria da
 * instituicao financeira -- informacao que nao aparece no texto do analista, apenas no cadastro.
 */
@Component
public class FuncionarioConsultaTool {

    private final EnquadramentoLookup lookup;

    FuncionarioConsultaTool(EnquadramentoLookup lookup) {
        this.lookup = lookup;
    }

    @Tool("Consulta no cadastro se a pessoa monitorada de uma ocorrencia de PLDFT e funcionaria da " +
            "instituicao financeira. Use o id numerico da ocorrencia (o mesmo que aparece como " +
            "'Ocorrencia: #X' no contexto da analise). Retorna o enquadramento real, que deve ser " +
            "usado para resolver qualquer pendencia de verificacao de vinculo com a instituicao " +
            "mencionada no parecer.")
    public String consultarFuncionario(@P("id da ocorrencia") long ocorrenciaId) {
        return lookup.porOcorrencia(ocorrenciaId)
                .map(Enquadramento::textoFuncionario)
                .orElse("Não foi possível localizar a pessoa monitorada da ocorrência #" + ocorrenciaId + " no cadastro.");
    }
}
