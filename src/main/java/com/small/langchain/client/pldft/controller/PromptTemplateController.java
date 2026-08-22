package com.small.langchain.client.pldft.controller;

import com.small.langchain.client.pldft.model.PromptTemplate;
import com.small.langchain.client.pldft.repository.PromptTemplateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/prompt-templates")
public class PromptTemplateController {

    private final PromptTemplateRepository repository;

    public PromptTemplateController(PromptTemplateRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<PromptTemplate> findAll(@RequestParam(required = false) Long produtoId) {
        return produtoId != null ? repository.findByProdutoId(produtoId) : repository.findAll();
    }

    @GetMapping("/produto/{produtoId}/ativo")
    public PromptTemplate findAtivoByProduto(@PathVariable Long produtoId) {
        return repository.findAtivoByProdutoId(produtoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nenhum prompt ativo cadastrado para esse produto"));
    }

    @PostMapping
    public ResponseEntity<PromptTemplate> criarNovaVersao(@RequestBody PromptTemplate template) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.criarNovaVersao(template));
    }
}
