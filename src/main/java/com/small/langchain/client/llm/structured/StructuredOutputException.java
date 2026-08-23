package com.small.langchain.client.llm.structured;

/** Falha ao obter do modelo um JSON valido no formato pedido. */
public class StructuredOutputException extends RuntimeException {

    public StructuredOutputException(String message, Throwable cause) {
        super(message, cause);
    }

    public StructuredOutputException(String message) {
        super(message);
    }
}
