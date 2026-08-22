package com.small.langchain.client.pldft.controller;

import com.small.langchain.client.pldft.model.Analise;
import com.small.langchain.client.pldft.repository.AnaliseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analises")
public class AnaliseController {

    private final AnaliseRepository repository;

    public AnaliseController(AnaliseRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Analise> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Analise> findById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Analise> create(@RequestBody Analise analise) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(analise));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Analise> update(@PathVariable Long id, @RequestBody Analise analise) {
        return repository.update(id, analise)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return repository.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
