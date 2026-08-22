package com.small.langchain.client.pldft.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Movimento(
        Long id,
        Long pessoaMonitoradaId,
        Long produtoId,
        BigDecimal valor,
        LocalDateTime dataMovimento,
        String tipoLancamento
) {
}
