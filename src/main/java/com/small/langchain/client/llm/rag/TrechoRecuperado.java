package com.small.langchain.client.llm.rag;

/**
 * Um trecho recuperado da base, com a fonte e o score de similaridade.
 *
 * <p>Carregar a fonte junto nao e detalhe: sem ela a resposta do modelo vira afirmacao sem
 * lastro, e o analista nao tem como conferir de onde saiu.
 */
public record TrechoRecuperado(String fonte, String texto, Double score) {
}
