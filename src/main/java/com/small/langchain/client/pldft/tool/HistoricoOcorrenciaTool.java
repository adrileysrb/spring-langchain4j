package com.small.langchain.client.pldft.tool;

import com.small.langchain.client.pldft.model.Ocorrencia;
import com.small.langchain.client.pldft.repository.OcorrenciaRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tool de reincidencia: mostra as outras ocorrencias da mesma pessoa monitorada. Um caso isolado
 * e um caso que se repete pedem niveis de diligencia diferentes, e essa e uma informacao que o
 * analista raramente escreve no rascunho porque ela nao esta na tela dele.
 */
@Component
public class HistoricoOcorrenciaTool {

    private final OcorrenciaRepository ocorrenciaRepository;

    HistoricoOcorrenciaTool(OcorrenciaRepository ocorrenciaRepository) {
        this.ocorrenciaRepository = ocorrenciaRepository;
    }

    @Tool("Consulta o historico da pessoa monitorada: quais outras ocorrencias de PLDFT ela ja teve, " +
            "com status e datas. Use o id numerico da ocorrencia atual. Serve para avaliar " +
            "reincidencia, que altera o nivel de diligencia exigido.")
    public String consultarHistorico(@P("id da ocorrencia atual") long ocorrenciaId) {
        Ocorrencia atual = ocorrenciaRepository.findById(ocorrenciaId).orElse(null);
        if (atual == null) {
            return "Ocorrência #" + ocorrenciaId + " não encontrada.";
        }

        List<Ocorrencia> anteriores = ocorrenciaRepository
                .findByPessoaMonitoradaId(atual.pessoaMonitoradaId())
                .stream()
                .filter(ocorrencia -> !ocorrencia.id().equals(atual.id()))
                .toList();

        if (anteriores.isEmpty()) {
            return "A pessoa monitorada da ocorrência #" + ocorrenciaId
                    + " não possui outras ocorrências registradas (caso isolado).";
        }

        String linhas = anteriores.stream()
                .map(ocorrencia -> "- Ocorrência #" + ocorrencia.id() + ": " + ocorrencia.status()
                        + ", aberta em " + FormatoPtBr.data(ocorrencia.dataAbertura())
                        + (ocorrencia.dataEncerramento() != null
                                ? ", encerrada em " + FormatoPtBr.data(ocorrencia.dataEncerramento())
                                : ""))
                .collect(Collectors.joining("\n"));

        return "A pessoa monitorada da ocorrência #" + ocorrenciaId + " possui "
                + anteriores.size() + " outra(s) ocorrência(s):\n" + linhas;
    }
}
