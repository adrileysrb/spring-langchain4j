package com.small.langchain.client.pldft.model;

import java.time.LocalDateTime;

public record Alerta(
        Long id,
        Long pessoaMonitoradaId,
        Long regraId,
        LocalDateTime dataGeracao,
        Long ocorrenciaId
) {
}
