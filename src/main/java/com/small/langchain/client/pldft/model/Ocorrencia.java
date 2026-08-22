package com.small.langchain.client.pldft.model;

import java.time.LocalDateTime;

public record Ocorrencia(
        Long id,
        Long pessoaMonitoradaId,
        Long produtoId,
        String status,
        LocalDateTime dataAbertura,
        LocalDateTime dataEncerramento
) {
}
