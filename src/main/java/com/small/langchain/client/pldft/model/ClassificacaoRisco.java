package com.small.langchain.client.pldft.model;

import java.util.List;

/**
 * Contrato da saida estruturada do modelo na classificacao de risco -- e este record que o
 * JsonSchema enviado ao provedor descreve, e nele que o JSON devolvido e desserializado.
 */
public record ClassificacaoRisco(
        String risco,
        Integer score,
        String justificativa,
        List<String> indicadores
) {

    public static final List<String> NIVEIS = List.of("BAIXO", "MEDIO", "ALTO", "CRITICO");
}
