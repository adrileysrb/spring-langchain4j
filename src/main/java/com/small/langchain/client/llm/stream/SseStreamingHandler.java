package com.small.langchain.client.llm.stream;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.function.Function;

/**
 * Adapter entre o {@link StreamingChatResponseHandler} do langchain4j e o {@link SseEmitter} do
 * Spring MVC: cada pedaco gerado pelo modelo vira um evento SSE.
 *
 * <p>Vale entender o que se troca ao streamar. No modo bloqueante da pra validar o texto inteiro
 * antes de qualquer um ver; aqui os tokens ja sairam quando a resposta termina. Guardrail em
 * streaming, portanto, nao protege o leitor -- protege o que sera <em>persistido</em>. Por isso a
 * validacao acontece em {@code onCompleteResponse}, e um resultado reprovado e sinalizado como
 * evento de erro em vez de gravado como se estivesse bom.
 *
 * <p>Os eventos emitidos sao {@code token} (pedaco de texto), {@code fim} (payload final, tipicamente
 * o registro persistido) e {@code erro}.
 */
public class SseStreamingHandler implements StreamingChatResponseHandler {

    private static final Logger log = LoggerFactory.getLogger(SseStreamingHandler.class);

    public static final String EVENTO_TOKEN = "token";
    public static final String EVENTO_FIM = "fim";
    public static final String EVENTO_ERRO = "erro";

    private final SseEmitter emitter;
    private final Function<String, Object> aoConcluir;
    private final StringBuilder acumulado = new StringBuilder();

    /**
     * @param aoConcluir recebe o texto completo e devolve o payload do evento final; e onde
     *                   guardrails e persistencia entram. Se lancar, vira evento de erro.
     */
    public SseStreamingHandler(SseEmitter emitter, Function<String, Object> aoConcluir) {
        this.emitter = emitter;
        this.aoConcluir = aoConcluir;
    }

    @Override
    public void onPartialResponse(String partialResponse) {
        acumulado.append(partialResponse);
        enviar(EVENTO_TOKEN, partialResponse);
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        try {
            Object payload = aoConcluir.apply(acumulado.toString());
            enviar(EVENTO_FIM, payload);
            emitter.complete();
        } catch (RuntimeException e) {
            log.warn("Falha ao finalizar o streaming: {}", e.getMessage());
            enviar(EVENTO_ERRO, e.getMessage());
            emitter.complete();
        }
    }

    @Override
    public void onError(Throwable error) {
        log.warn("Erro durante o streaming do modelo: {}", error.getMessage());
        enviar(EVENTO_ERRO, error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName());
        emitter.complete();
    }

    /**
     * Falha de envio significa cliente desconectado -- situacao normal em SSE, nao erro da
     * aplicacao. Encerra o emitter e para de tentar.
     */
    private void enviar(String evento, Object dados) {
        try {
            emitter.send(SseEmitter.event().name(evento).data(dados));
        } catch (IOException | IllegalStateException e) {
            log.debug("Cliente SSE desconectado durante o evento '{}'", evento);
            emitter.complete();
        }
    }
}
