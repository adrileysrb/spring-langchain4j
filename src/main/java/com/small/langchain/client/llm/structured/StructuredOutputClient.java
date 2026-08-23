package com.small.langchain.client.llm.structured;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Obtem do modelo uma resposta em JSON e a converte num record Java.
 *
 * <p>Usa {@link ResponseFormat} com {@link JsonSchema}, que e o mecanismo do langchain4j para
 * saida estruturada: o schema vai junto da requisicao e o provedor restringe a geracao a ele.
 * Duas defesas praticas acompanham isso, porque na vida real o caminho feliz falha:
 * <ol>
 *   <li>se o provedor nao suportar json_schema, a chamada e repetida com o schema descrito
 *       em linguagem natural dentro do proprio prompt;</li>
 *   <li>o texto e limpo antes do parse -- modelos pequenos adoram embrulhar o JSON em cercas
 *       de markdown ou emendar um comentario depois do objeto.</li>
 * </ol>
 */
@Service
public class StructuredOutputClient {

    private static final Logger log = LoggerFactory.getLogger(StructuredOutputClient.class);

    /**
     * Mapper proprio, e nao o do Spring MVC, por duas razoes: a serializacao HTTP e a leitura da
     * saida do modelo sao contratos independentes (mexer numa nao deveria alterar a outra), e a
     * leitura aqui precisa ser deliberadamente tolerante -- modelo pequeno erra virgula final e
     * troca aspas duplas por simples com frequencia.
     */
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public <R> R executar(ChatModel chatModel, List<ChatMessage> mensagens, JsonSchema schema, Class<R> tipo) {
        ResponseFormat formatoEstrito = ResponseFormat.builder()
                .type(ResponseFormatType.JSON)
                .jsonSchema(schema)
                .build();
        try {
            return converter(chamar(chatModel, mensagens, formatoEstrito), tipo);
        } catch (RuntimeException e) {
            log.warn("Saída estruturada nativa falhou ({}), repetindo com o schema descrito no prompt",
                    e.getMessage());
        }

        try {
            return converter(chamar(chatModel, comSchemaNoPrompt(mensagens, schema), ResponseFormat.JSON), tipo);
        } catch (RuntimeException e) {
            throw new StructuredOutputException(
                    "O modelo não devolveu um JSON válido no formato esperado: " + e.getMessage(), e);
        }
    }

    private String chamar(ChatModel chatModel, List<ChatMessage> mensagens, ResponseFormat formato) {
        ChatRequest request = ChatRequest.builder()
                .messages(mensagens)
                .responseFormat(formato)
                .build();
        ChatResponse response = chatModel.chat(request);
        String texto = response.aiMessage().text();
        if (texto == null || texto.isBlank()) {
            throw new StructuredOutputException("resposta vazia (possivelmente truncada por max_tokens)");
        }
        return texto;
    }

    private <R> R converter(String texto, Class<R> tipo) {
        String json = extrairObjetoJson(texto);
        try {
            return OBJECT_MAPPER.readValue(json, tipo);
        } catch (Exception e) {
            throw new StructuredOutputException("JSON inválido: " + e.getMessage(), e);
        }
    }

    /** Descarta cercas de markdown e qualquer texto antes/depois do objeto JSON. */
    private String extrairObjetoJson(String texto) {
        int inicio = texto.indexOf('{');
        int fim = texto.lastIndexOf('}');
        if (inicio < 0 || fim <= inicio) {
            throw new StructuredOutputException(
                    "nenhum objeto JSON encontrado na resposta: " + resumir(texto));
        }
        return texto.substring(inicio, fim + 1);
    }

    private List<ChatMessage> comSchemaNoPrompt(List<ChatMessage> mensagens, JsonSchema schema) {
        List<ChatMessage> comInstrucao = new ArrayList<>(mensagens);
        String campos = schema.rootElement() instanceof JsonObjectSchema objeto
                ? JsonSchemaDescriber.descrever(objeto)
                : "";
        comInstrucao.add(UserMessage.from(
                "Responda EXCLUSIVAMENTE com um objeto JSON válido, sem markdown e sem texto ao redor, "
                        + "com exatamente estes campos:\n" + campos));
        return comInstrucao;
    }

    private String resumir(String texto) {
        String limpo = texto.strip().replaceAll("\\s+", " ");
        return limpo.length() > 200 ? limpo.substring(0, 200) + "..." : limpo;
    }
}
