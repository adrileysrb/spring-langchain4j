package com.small.langchain.client.llm.observability;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Observer de todas as chamadas ao modelo: e a interface {@link ChatModelListener} do langchain4j
 * que permite instrumentar tokens, latencia e erros sem que nenhum servico saiba disso -- basta
 * ser um bean, que a {@link com.small.langchain.client.llm.model.ChatModelFactory} pluga sozinha.
 *
 * <p>O mapa {@code attributes()} e compartilhado entre {@code onRequest} e {@code onResponse}/
 * {@code onError} da mesma chamada; e por ele que o instante inicial atravessa pro outro lado.
 */
@Component
public class LlmCallRecordingListener implements ChatModelListener {

    private static final Logger log = LoggerFactory.getLogger(LlmCallRecordingListener.class);

    private static final String ATRIBUTO_INICIO = "llm.inicioNanos";
    private static final String ATRIBUTO_TAREFA = "llm.tarefa";
    private static final int TAMANHO_MAXIMO_ERRO = 500;

    private final LlmCallRepository repository;

    public LlmCallRecordingListener(LlmCallRepository repository) {
        this.repository = repository;
    }

    @Override
    public void onRequest(ChatModelRequestContext context) {
        context.attributes().put(ATRIBUTO_INICIO, System.nanoTime());
        // A tarefa e lida aqui (ainda na thread de quem chamou) porque o callback de resposta
        // pode rodar em outra thread, onde o ThreadLocal ja nao vale.
        context.attributes().put(ATRIBUTO_TAREFA, LlmTaskContext.atual());
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        ChatResponseMetadata metadata = context.chatResponse().metadata();
        TokenUsage tokens = metadata != null ? metadata.tokenUsage() : null;
        FinishReason finishReason = metadata != null ? metadata.finishReason() : null;

        registrar(new LlmCall(
                null,
                tarefa(context.attributes()),
                modelo(metadata, context.chatRequest()),
                tokens != null ? tokens.inputTokenCount() : null,
                tokens != null ? tokens.outputTokenCount() : null,
                tokens != null ? tokens.totalTokenCount() : null,
                duracaoMs(context.attributes()),
                finishReason != null ? finishReason.name() : null,
                true,
                null,
                LocalDateTime.now()
        ));
    }

    @Override
    public void onError(ChatModelErrorContext context) {
        Throwable erro = context.error();
        String mensagem = erro.getMessage() != null ? erro.getMessage() : erro.getClass().getSimpleName();

        registrar(new LlmCall(
                null,
                tarefa(context.attributes()),
                modelo(null, context.chatRequest()),
                null, null, null,
                duracaoMs(context.attributes()),
                null,
                false,
                mensagem.length() > TAMANHO_MAXIMO_ERRO ? mensagem.substring(0, TAMANHO_MAXIMO_ERRO) : mensagem,
                LocalDateTime.now()
        ));
    }

    /**
     * Observabilidade nunca pode derrubar a funcionalidade que ela observa: se gravar a metrica
     * falhar, o erro fica no log e a chamada ao modelo segue normalmente.
     */
    private void registrar(LlmCall call) {
        try {
            repository.save(call);
        } catch (RuntimeException e) {
            log.warn("Não foi possível registrar a métrica da chamada de IA: {}", e.getMessage());
        }
    }

    private String tarefa(Map<Object, Object> atributos) {
        Object tarefa = atributos.get(ATRIBUTO_TAREFA);
        return tarefa instanceof String texto ? texto : LlmTaskContext.DESCONHECIDA;
    }

    private Long duracaoMs(Map<Object, Object> atributos) {
        Object inicio = atributos.get(ATRIBUTO_INICIO);
        return inicio instanceof Long inicioNanos ? (System.nanoTime() - inicioNanos) / 1_000_000 : null;
    }

    private String modelo(ChatResponseMetadata metadata, ChatRequest request) {
        if (metadata != null && metadata.modelName() != null) {
            return metadata.modelName();
        }
        ChatRequestParameters parameters = request != null ? request.parameters() : null;
        return parameters != null ? parameters.modelName() : null;
    }
}
