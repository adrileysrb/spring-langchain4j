package com.small.langchain.client.pldft.controller;

import com.small.langchain.client.pldft.model.Alerta;
import com.small.langchain.client.pldft.repository.AlertaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private final AlertaRepository repository;

    public AlertaController(AlertaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Alerta> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Alerta> findById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Alerta> create(@RequestBody Alerta alerta) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(alerta));
    }
}
