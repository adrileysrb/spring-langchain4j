package com.small.langchain.client.llm.tool;

import dev.langchain4j.model.chat.request.ToolChoice;

/**
 * Strategy que define como o loop de tool-calling se comporta: com que {@link ToolChoice} cada
 * rodada e enviada e quando parar.
 *
 * <p>Os dois modos cobrem necessidades bem diferentes, e por isso nao dava pra deixar isso
 * fixo dentro do runner:
 * <ul>
 *   <li>conversa livre -- o modelo decide se usa ferramenta e o loop acaba quando ele responde
 *       em texto ({@link #ateOModeloParar()});</li>
 *   <li>coleta determinista -- a aplicacao ja sabe que precisa do resultado de todas as tools
 *       e insiste ate consegui-los ({@link #ateChamarTodasAsTools()}).</li>
 * </ul>
 */
public interface ToolLoopPolicy {

    ToolChoice toolChoice();

    boolean deveContinuar(ToolLoopState estado);

    String descricao();

    static ToolLoopPolicy ateOModeloParar() {
        return new AutoToolLoopPolicy();
    }

    static ToolLoopPolicy ateChamarTodasAsTools() {
        return new RequiredAllToolLoopPolicy();
    }
}
