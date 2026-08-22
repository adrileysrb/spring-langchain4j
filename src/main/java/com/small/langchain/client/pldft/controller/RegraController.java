package com.small.langchain.client.pldft.controller;

import com.small.langchain.client.pldft.model.Regra;
import com.small.langchain.client.pldft.repository.RegraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regras")
public class RegraController {

    private final RegraRepository repository;

    public RegraController(RegraRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Regra> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Regra> findById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Regra> create(@RequestBody Regra regra) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(regra));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Regra> update(@PathVariable Long id, @RequestBody Regra regra) {
        return repository.update(id, regra)
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
