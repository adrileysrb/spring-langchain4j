package com.small.langchain.client.llm.tool;

import dev.langchain4j.model.chat.request.ToolChoice;

/**
 * Modo conversa: o modelo escolhe se chama alguma ferramenta ({@link ToolChoice#AUTO}) e o loop
 * segue enquanto ele continuar pedindo tools. Quando responde so texto, aquele texto e a resposta.
 */
final class AutoToolLoopPolicy implements ToolLoopPolicy {

    @Override
    public ToolChoice toolChoice() {
        return ToolChoice.AUTO;
    }

    @Override
    public boolean deveContinuar(ToolLoopState estado) {
        return estado.ultimaRodadaChamouTool() && estado.temRodadaSobrando();
    }

    @Override
    public String descricao() {
        return "até o modelo parar de pedir ferramentas";
    }
}
