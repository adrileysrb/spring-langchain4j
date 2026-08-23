package com.small.langchain.client.pldft.service;

/**
 * Entrada do extrator de risco: os fatos que as tools levantaram sobre a ocorrencia, ja em texto.
 *
 * <p>Manter isso num record separado deixa claro o contrato entre as duas etapas -- coletar
 * (tools) e interpretar (saida estruturada) -- e permite testar a segunda sem chamar a primeira.
 */
public record ContextoOcorrencia(
        Long ocorrenciaId,
        String produto,
        String status,
        String fatosLevantados,
        String ultimoParecer
) {
}
