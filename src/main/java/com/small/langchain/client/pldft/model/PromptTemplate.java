package com.small.langchain.client.pldft.model;

import java.time.LocalDateTime;

public record PromptTemplate(
        Long id,
        Long produtoId,
        Integer versao,
        String nome,
        String promptSistema,
        String promptUsuario,
        String modelo,
        Double temperature,
        Integer maxTokens,
        Boolean ativo,
        LocalDateTime criadoEm
) {
}
