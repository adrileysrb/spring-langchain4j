package com.small.langchain.client.llm.tool;

import java.util.Set;

/**
 * Estado do loop de tools apos uma rodada. E o que a {@link ToolLoopPolicy} recebe pra decidir
 * se vale rodar mais uma volta.
 */
public record ToolLoopState(
        int rodada,
        int maxRodadas,
        Set<String> toolsJaChamadas,
        int totalDeTools,
        boolean ultimaRodadaChamouTool
) {

    public boolean temRodadaSobrando() {
        return rodada < maxRodadas;
    }

    public boolean todasAsToolsForamChamadas() {
        return toolsJaChamadas.size() >= totalDeTools;
    }
}
