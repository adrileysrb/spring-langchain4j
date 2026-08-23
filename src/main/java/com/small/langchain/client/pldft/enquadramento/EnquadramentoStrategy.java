package com.small.langchain.client.pldft.enquadramento;

import com.small.langchain.client.pldft.model.Ocorrencia;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Duas maneiras de obter o mesmo dado obrigatorio, selecionaveis por feature flag
 * ({@code pldft.enquadramento-via-tools}).
 *
 * <p>Vale explicar por que existe escolha aqui. O enquadramento e sempre necessario e vem de uma
 * consulta deterministica ao cadastro -- nao ha decisao a tomar. Roteirizar isso pelo modelo
 * ({@link EnquadramentoViaTools}) e o que demonstra tool calling de verdade, mas custa uma ou mais
 * chamadas de inferencia, adiciona latencia e traz um modo de falha proprio: modelo local pequeno
 * as vezes "finge" ter chamado a ferramenta, escrevendo texto solto em vez de emitir um
 * {@code tool_call} -- daí a politica que insiste com {@code ToolChoice.REQUIRED}.
 *
 * <p>{@link EnquadramentoDireto} simplesmente le o banco: zero token, zero latencia, zero chance
 * de o modelo inventar. Por isso e o padrao. A flag existe para tornar a comparacao concreta em
 * vez de teorica -- com as metricas em {@code /api/llm/metricas} da pra medir exatamente o que a
 * abordagem por tools custa neste fluxo.
 *
 * <p>A licao nao e "tools sao ruins": e que tool calling paga quando o modelo precisa <em>decidir</em>
 * o que consultar (como no assistente do analista, onde a pergunta e livre), e nao quando a
 * aplicacao ja sabe de antemao qual dado buscar.
 */
public interface EnquadramentoStrategy {

    /** Identificador da abordagem, para log e diagnostico. */
    String nome();

    /**
     * @param chatModel modelo configurado para o prompt em uso; ignorado pelas abordagens que
     *                  nao passam pela LLM
     * @return texto do enquadramento pronto para o prompt, ou {@code null} se nao foi possivel obter
     */
    String consultar(Ocorrencia ocorrencia, ChatModel chatModel);
}
