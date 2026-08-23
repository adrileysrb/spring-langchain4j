package com.small.langchain.client.llm.observability;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/llm")
public class LlmObservabilityController {

    private final LlmCallRepository repository;

    public LlmObservabilityController(LlmCallRepository repository) {
        this.repository = repository;
    }

    /** Consumo consolidado por tarefa: quanto cada funcionalidade de IA custa em tokens e tempo. */
    @GetMapping("/metricas")
    public List<LlmCallStats> metricas() {
        return repository.estatisticasPorTarefa();
    }

    /** Trilha das ultimas chamadas, util pra depurar prompt que trunca ou modelo que nao responde. */
    @GetMapping("/chamadas")
    public List<LlmCall> chamadas(@RequestParam(defaultValue = "50") int limite) {
        return repository.ultimas(Math.clamp(limite, 1, 500));
    }
}
