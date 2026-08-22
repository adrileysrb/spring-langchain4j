package com.small.langchain.client.pldft.model;

import java.time.LocalDateTime;

public record AnaliseMelhoria(
        Long id,
        Long analiseId,
        Long promptTemplateId,
        String textoOriginal,
        String textoSugerido,
        String status,
        String erroMensagem,
        LocalDateTime solicitadoEm,
        LocalDateTime respondidoEm
) {
}
