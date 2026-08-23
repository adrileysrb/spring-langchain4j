package com.small.langchain.client.llm.structured;

import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

import java.util.List;
import java.util.Map;

/**
 * Traduz um {@link JsonObjectSchema} para instrucao em linguagem natural.
 *
 * <p>Serve ao plano B do {@link StructuredOutputClient}: nem todo provedor aceita
 * {@code response_format: json_schema} -- e modelos locais pequenos costumam ser justamente os
 * que nao aceitam. Quando o caminho nativo falha, descrever o mesmo schema dentro do prompt
 * ainda leva o modelo ao formato certo na maioria das vezes.
 */
final class JsonSchemaDescriber {

    private JsonSchemaDescriber() {
    }

    static String descrever(JsonObjectSchema schema) {
        Map<String, JsonSchemaElement> propriedades = schema.properties();
        List<String> obrigatorias = schema.required() != null ? schema.required() : List.of();

        StringBuilder texto = new StringBuilder();
        propriedades.forEach((nome, elemento) -> {
            texto.append("- \"").append(nome).append("\": ").append(tipo(elemento));
            if (obrigatorias.contains(nome)) {
                texto.append(" (obrigatório)");
            }
            String descricao = descricao(elemento);
            if (descricao != null && !descricao.isBlank()) {
                texto.append(" -- ").append(descricao);
            }
            texto.append('\n');
        });
        return texto.toString();
    }

    private static String tipo(JsonSchemaElement elemento) {
        return switch (elemento) {
            case JsonEnumSchema enumSchema -> "um valor entre " + String.join(", ", enumSchema.enumValues());
            case JsonStringSchema ignored -> "texto";
            case JsonIntegerSchema ignored -> "número inteiro";
            case JsonNumberSchema ignored -> "número";
            case JsonBooleanSchema ignored -> "true ou false";
            case JsonArraySchema arraySchema -> "lista de " + tipo(arraySchema.items());
            case JsonObjectSchema ignored -> "objeto";
            default -> "valor";
        };
    }

    private static String descricao(JsonSchemaElement elemento) {
        return switch (elemento) {
            case JsonEnumSchema schema -> schema.description();
            case JsonStringSchema schema -> schema.description();
            case JsonIntegerSchema schema -> schema.description();
            case JsonNumberSchema schema -> schema.description();
            case JsonBooleanSchema schema -> schema.description();
            case JsonArraySchema schema -> schema.description();
            case JsonObjectSchema schema -> schema.description();
            default -> null;
        };
    }
}
