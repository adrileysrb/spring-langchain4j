package com.small.langchain.client.pldft.model;

import java.time.LocalDateTime;

public record Analise(
        Long id,
        Long ocorrenciaId,
        String analista,
        LocalDateTime dataAnalise,
        String parecer
) {
}
