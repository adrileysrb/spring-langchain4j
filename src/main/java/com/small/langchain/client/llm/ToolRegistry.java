package com.small.langchain.client.llm;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agrupa um conjunto de objetos anotados com {@code @Tool} (API de baixo nivel do
 * langchain4j), expondo as {@link ToolSpecification} correspondentes para o
 * {@link dev.langchain4j.model.chat.request.ChatRequest} e sabendo executar de volta
 * a tool certa a partir de um {@link ToolExecutionRequest} vindo da resposta do modelo.
 */
public final class ToolRegistry {

    private final Map<String, Object> toolInstanceByName = new HashMap<>();
    private final List<ToolSpecification> specifications = new ArrayList<>();

    private ToolRegistry() {
    }

    public static ToolRegistry of(Object... tools) {
        ToolRegistry registry = new ToolRegistry();
        for (Object tool : tools) {
            for (ToolSpecification specification : ToolSpecifications.toolSpecificationsFrom(tool)) {
                registry.toolInstanceByName.put(specification.name(), tool);
                registry.specifications.add(specification);
            }
        }
        return registry;
    }

    public List<ToolSpecification> specifications() {
        return specifications;
    }

    public String execute(ToolExecutionRequest request) {
        Object toolInstance = toolInstanceByName.get(request.name());
        if (toolInstance == null) {
            return "Erro: ferramenta '" + request.name() + "' não encontrada.";
        }
        return new DefaultToolExecutor(toolInstance, request).execute(request, null);
    }
}
