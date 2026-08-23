package com.small.langchain.client.pldft.model;

import java.time.LocalDateTime;
import java.util.List;

/** Classificacao de risco persistida, com a rastreabilidade de qual modelo a produziu. */
public record OcorrenciaClassificacao(
        Long id,
        Long ocorrenciaId,
        String risco,
        Integer score,
        String justificativa,
        List<String> indicadores,
        String modelo,
        LocalDateTime criadoEm
) {
}
