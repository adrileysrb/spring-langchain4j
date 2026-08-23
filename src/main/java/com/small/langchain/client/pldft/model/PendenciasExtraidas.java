package com.small.langchain.client.pldft.model;

import java.util.List;

/**
 * Raiz da saida estruturada da extracao de pendencias. O schema JSON exige um objeto no topo,
 * entao a lista vem envelopada em vez de solta.
 */
public record PendenciasExtraidas(List<PendenciaItem> pendencias) {

    public List<PendenciaItem> pendenciasOuVazio() {
        return pendencias != null ? pendencias : List.of();
    }
}
