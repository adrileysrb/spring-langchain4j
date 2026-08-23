package com.small.langchain.client.llm.tool;

import dev.langchain4j.model.chat.request.ToolChoice;

/**
 * Modo coleta: força uma chamada de ferramenta por rodada ({@link ToolChoice#REQUIRED}) e insiste
 * até que todas as tools do registry tenham sido executadas pelo menos uma vez.
 *
 * <p>Existe porque modelos locais pequenos às vezes "fingem" ter usado a ferramenta escrevendo
 * texto solto em vez de emitir um tool_call de verdade; quando o dado é obrigatório para o
 * fluxo, deixar essa decisão com o modelo não é opção.
 */
final class RequiredAllToolLoopPolicy implements ToolLoopPolicy {

    @Override
    public ToolChoice toolChoice() {
        return ToolChoice.REQUIRED;
    }

    @Override
    public boolean deveContinuar(ToolLoopState estado) {
        return !estado.todasAsToolsForamChamadas() && estado.temRodadaSobrando();
    }

    @Override
    public String descricao() {
        return "até todas as ferramentas serem chamadas";
    }
}
