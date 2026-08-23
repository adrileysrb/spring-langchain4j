package com.small.langchain.client.pldft.controller;

import com.small.langchain.client.pldft.dto.AtualizarStatusRequest;
import com.small.langchain.client.pldft.model.AnaliseMelhoria;
import com.small.langchain.client.pldft.service.AnaliseMelhoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/analises/{analiseId}/melhorias")
public class AnaliseMelhoriaController {

    private final AnaliseMelhoriaService service;

    public AnaliseMelhoriaController(AnaliseMelhoriaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AnaliseMelhoria> gerar(@PathVariable Long analiseId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.gerar(analiseId));
    }

    /**
     * Mesma geracao, entregue token a token. Eventos: {@code token}, {@code fim} e {@code erro}.
     * Em modelo local, onde o parecer inteiro leva dezenas de segundos, e a diferenca entre uma
     * tela parada e uma tela escrevendo.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter gerarComStream(@PathVariable Long analiseId) {
        return service.gerarComStream(analiseId);
    }

    @GetMapping
    public List<AnaliseMelhoria> listar(@PathVariable Long analiseId) {
        return service.listar(analiseId);
    }

    @PutMapping("/{id}/status")
    public AnaliseMelhoria atualizarStatus(
            @PathVariable Long analiseId,
            @PathVariable Long id,
            @RequestBody AtualizarStatusRequest request
    ) {
        return service.atualizarStatus(analiseId, id, request.status());
    }
}
