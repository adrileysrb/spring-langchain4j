package com.small.langchain.client.pldft.controller;

import com.small.langchain.client.pldft.model.Movimento;
import com.small.langchain.client.pldft.repository.MovimentoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movimentos")
public class MovimentoController {

    private final MovimentoRepository repository;

    public MovimentoController(MovimentoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Movimento> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Movimento> findById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Movimento> create(@RequestBody Movimento movimento) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(movimento));
    }
}
