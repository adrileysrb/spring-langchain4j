package com.small.langchain.client.llm.structured;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;

import java.util.List;

/**
 * Template Method para extrair informacao estruturada de um texto.
 *
 * <p>O algoritmo -- montar o prompt, chamar o modelo restringindo a saida ao schema, tratar as
 * falhas de formato e converter o JSON num record -- e sempre o mesmo e fica fixo aqui. Cada
 * extrator concreto so responde as tres perguntas que variam: qual o schema, o que perguntar
 * e em que tipo cai a resposta.
 *
 * @param <I> entrada de dominio (a ocorrencia, a analise...)
 * @param <O> record que espelha o JSON devolvido pelo modelo
 */
public abstract class StructuredExtractor<I, O> {

    private final StructuredOutputClient client;

    protected StructuredExtractor(StructuredOutputClient client) {
        this.client = client;
    }

    public final O extrair(ChatModel chatModel, I entrada) {
        JsonSchema schema = JsonSchema.builder()
                .name(nomeDoSchema())
                .rootElement(schema())
                .build();
        return client.executar(chatModel, mensagens(entrada), schema, tipoDaResposta());
    }

    /** Nome do schema; alguns provedores o exibem em erro de validacao, entao vale ser descritivo. */
    protected abstract String nomeDoSchema();

    protected abstract JsonObjectSchema schema();

    protected abstract List<ChatMessage> mensagens(I entrada);

    protected abstract Class<O> tipoDaResposta();
}
