package com.small.langchain.client.pldft.controller;

import com.small.langchain.client.pldft.model.PessoaMonitorada;
import com.small.langchain.client.pldft.repository.PessoaMonitoradaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pessoas-monitoradas")
public class PessoaMonitoradaController {

    private final PessoaMonitoradaRepository repository;

    public PessoaMonitoradaController(PessoaMonitoradaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<PessoaMonitorada> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PessoaMonitorada> findById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PessoaMonitorada> create(@RequestBody PessoaMonitorada pessoa) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(pessoa));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PessoaMonitorada> update(@PathVariable Long id, @RequestBody PessoaMonitorada pessoa) {
        return repository.update(id, pessoa)
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
