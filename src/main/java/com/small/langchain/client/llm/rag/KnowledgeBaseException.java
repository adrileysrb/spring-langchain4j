package com.small.langchain.client.llm.rag;

/** Falha ao indexar ou consultar a base de conhecimento. */
public class KnowledgeBaseException extends RuntimeException {

    public KnowledgeBaseException(String message) {
        super(message);
    }

    public KnowledgeBaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
