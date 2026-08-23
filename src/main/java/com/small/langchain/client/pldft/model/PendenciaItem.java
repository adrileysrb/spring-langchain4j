package com.small.langchain.client.pldft.model;

import java.util.List;

/** Uma pendencia identificada no parecer, no formato em que o modelo a devolve. */
public record PendenciaItem(String descricao, String tipo, String prioridade) {

    public static final List<String> TIPOS = List.of(
            "DOCUMENTACAO", "CONTATO_CLIENTE", "VERIFICACAO_CADASTRAL", "ESCALONAMENTO", "OUTRO");

    public static final List<String> PRIORIDADES = List.of("ALTA", "MEDIA", "BAIXA");
}
