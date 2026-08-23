package com.small.langchain.client.pldft.dto;

import com.small.langchain.client.llm.rag.TrechoRecuperado;

import java.util.List;

/**
 * A resposta vem sempre acompanhada dos trechos que a embasaram. Em conformidade, resposta sem
 * fonte nao serve: o analista precisa poder abrir o normativo e conferir.
 */
public record RespostaNormativoDto(String resposta, List<TrechoRecuperado> fontes) {
}
