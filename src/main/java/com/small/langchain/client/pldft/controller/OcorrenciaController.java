package com.small.langchain.client.pldft.controller;

import com.small.langchain.client.pldft.model.Ocorrencia;
import com.small.langchain.client.pldft.repository.OcorrenciaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ocorrencias")
public class OcorrenciaController {

    private final OcorrenciaRepository repository;

    public OcorrenciaController(OcorrenciaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Ocorrencia> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ocorrencia> findById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Ocorrencia> create(@RequestBody Ocorrencia ocorrencia) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(ocorrencia));
    }
}
