package com.small.langchain.client.pldft.dto;

import java.util.List;

/**
 * Alem da resposta, devolve quais ferramentas foram consultadas e quantas mensagens ja existem
 * na memoria -- num assistente que consulta dados, saber de onde veio a resposta faz parte dela.
 */
public record RespostaAssistenteDto(
        String conversaId,
        String resposta,
        List<String> ferramentasConsultadas,
        int mensagensNaMemoria
) {
}
