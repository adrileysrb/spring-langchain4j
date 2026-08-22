package com.small.langchain.client.pldft.controller;

import com.small.langchain.client.pldft.dto.AtualizarStatusRequest;
import com.small.langchain.client.pldft.model.AnaliseMelhoria;
import com.small.langchain.client.pldft.service.AnaliseMelhoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
