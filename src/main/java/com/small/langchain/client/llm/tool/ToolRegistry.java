package com.small.langchain.client.llm.tool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry das tools disponiveis numa conversa. Guarda os dois lados que a API de baixo nivel
 * do langchain4j mantem separados: a {@link ToolSpecification} que vai no {@code ChatRequest}
 * (o que o modelo enxerga) e o objeto Java que sabe executar aquela tool quando o modelo pede.
 *
 * <p>Montar um registry por caso de uso, e nao um global, e o que permite expor um conjunto
 * diferente de ferramentas em cada fluxo.
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

    public int size() {
        return specifications.size();
    }

    /**
     * Executa a tool pedida pelo modelo. O {@link DefaultToolExecutor} cuida de desserializar
     * os argumentos JSON e casar com os parametros do metodo anotado com {@code @Tool}.
     */
    public String execute(ToolExecutionRequest request) {
        Object toolInstance = toolInstanceByName.get(request.name());
        if (toolInstance == null) {
            return "Erro: ferramenta '" + request.name() + "' não encontrada.";
        }
        return new DefaultToolExecutor(toolInstance, request).execute(request, null);
    }
}
