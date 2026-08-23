package com.small.langchain.client.pldft.controller;

import com.small.langchain.client.pldft.model.AnalisePendencia;
import com.small.langchain.client.pldft.service.PendenciaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analises/{analiseId}/pendencias")
public class PendenciaController {

    private final PendenciaService service;

    public PendenciaController(PendenciaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<List<AnalisePendencia>> extrair(@PathVariable Long analiseId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.extrair(analiseId));
    }

    @GetMapping
    public List<AnalisePendencia> listar(@PathVariable Long analiseId) {
        return service.listar(analiseId);
    }
}
