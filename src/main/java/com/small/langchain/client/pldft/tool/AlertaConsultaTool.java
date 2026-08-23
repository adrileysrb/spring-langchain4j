package com.small.langchain.client.pldft.tool;

import com.small.langchain.client.pldft.model.Alerta;
import com.small.langchain.client.pldft.model.Regra;
import com.small.langchain.client.pldft.repository.AlertaRepository;
import com.small.langchain.client.pldft.repository.RegraRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tool que lista as regras de monitoramento que dispararam alerta numa ocorrencia -- ou seja,
 * o motivo pelo qual o caso existe. E a informacao que ancora o parecer no que foi detectado,
 * evitando que o modelo invente uma justificativa plausivel porem errada.
 */
@Component
public class AlertaConsultaTool {

    private final AlertaRepository alertaRepository;
    private final RegraRepository regraRepository;

    AlertaConsultaTool(AlertaRepository alertaRepository, RegraRepository regraRepository) {
        this.alertaRepository = alertaRepository;
        this.regraRepository = regraRepository;
    }

    @Tool("Lista os alertas que originaram uma ocorrencia de PLDFT, com o codigo e a descricao " +
            "da regra de monitoramento que disparou cada um. Use o id numerico da ocorrencia. " +
            "Serve para fundamentar o parecer no que foi efetivamente detectado.")
    public String consultarAlertas(@P("id da ocorrencia") long ocorrenciaId) {
        List<Alerta> alertas = alertaRepository.findByOcorrenciaId(ocorrenciaId);
        if (alertas.isEmpty()) {
            return "Nenhum alerta vinculado à ocorrência #" + ocorrenciaId + ".";
        }

        Map<Long, Regra> regrasPorId = regraRepository.findAll().stream()
                .collect(Collectors.toMap(Regra::id, Function.identity()));

        String linhas = alertas.stream()
                .map(alerta -> {
                    Regra regra = regrasPorId.get(alerta.regraId());
                    String descricaoRegra = regra != null
                            ? regra.codigo() + " (" + regra.descricao() + ")"
                            : "regra #" + alerta.regraId() + " não encontrada";
                    return "- " + descricaoRegra + ", gerado em " + FormatoPtBr.data(alerta.dataGeracao());
                })
                .collect(Collectors.joining("\n"));

        return "Ocorrência #" + ocorrenciaId + " possui " + alertas.size() + " alerta(s):\n" + linhas;
    }
}
