package com.small.langchain.client.pldft.tool;

import com.small.langchain.client.pldft.model.Ocorrencia;
import com.small.langchain.client.pldft.model.PessoaMonitorada;
import com.small.langchain.client.pldft.repository.OcorrenciaRepository;
import com.small.langchain.client.pldft.repository.PessoaMonitoradaRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * Tool exposta a LLM durante a revisao do parecer de uma analise: permite consultar,
 * a partir do id da ocorrencia, se a pessoa monitorada correspondente e PEP, PEM
 * e/ou funcionaria da instituicao -- dado que essa informacao nao aparece no texto
 * bruto do analista de proposito (fica so no cadastro).
 */
public class PessoaEnquadramentoTools {

    private final OcorrenciaRepository ocorrenciaRepository;
    private final PessoaMonitoradaRepository pessoaMonitoradaRepository;

    public PessoaEnquadramentoTools(
            OcorrenciaRepository ocorrenciaRepository,
            PessoaMonitoradaRepository pessoaMonitoradaRepository
    ) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.pessoaMonitoradaRepository = pessoaMonitoradaRepository;
    }

    @Tool("Consulta no cadastro se a pessoa monitorada de uma ocorrencia de PLDFT e PEP " +
            "(Pessoa Politicamente Exposta), PEM (Pessoa Exposta na Midia) e/ou funcionaria " +
            "da instituicao financeira. Use o id numerico da ocorrencia (o mesmo que aparece " +
            "como 'Ocorrencia: #X' no contexto da analise). Retorna o enquadramento real, " +
            "que deve ser usado para resolver qualquer pendencia de verificacao mencionada no parecer.")
    public String consultarEnquadramentoPorOcorrencia(@P("id da ocorrencia") long ocorrenciaId) {
        Ocorrencia ocorrencia = ocorrenciaRepository.findById(ocorrenciaId).orElse(null);
        if (ocorrencia == null) {
            return "Ocorrencia #" + ocorrenciaId + " nao encontrada no cadastro.";
        }

        PessoaMonitorada pessoa = pessoaMonitoradaRepository.findById(ocorrencia.pessoaMonitoradaId()).orElse(null);
        if (pessoa == null) {
            return "Pessoa monitorada da ocorrencia #" + ocorrenciaId + " nao encontrada no cadastro.";
        }

        return "Pessoa monitorada: " + pessoa.nome()
                + " | PEP (Pessoa Politicamente Exposta): " + textoBooleano(pessoa.pep())
                + " | PEM (Pessoa Exposta na Midia): " + textoBooleano(pessoa.pem())
                + " | Funcionario da instituicao: " + textoBooleano(pessoa.funcionario());
    }

    private String textoBooleano(Boolean valor) {
        return Boolean.TRUE.equals(valor) ? "sim" : "nao";
    }
}
