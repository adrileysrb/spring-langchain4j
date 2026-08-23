package com.small.langchain.client.pldft.model;

import java.time.LocalDateTime;

/** Pendencia extraida de um parecer e persistida para virar tarefa do analista. */
public record AnalisePendencia(
        Long id,
        Long analiseId,
        String descricao,
        String tipo,
        String prioridade,
        LocalDateTime criadoEm
) {
}
