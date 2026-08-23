package com.small.langchain.client.pldft.controller;

import com.small.langchain.client.pldft.model.OcorrenciaClassificacao;
import com.small.langchain.client.pldft.service.ClassificacaoRiscoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ocorrencias/{ocorrenciaId}/classificacoes")
public class ClassificacaoRiscoController {

    private final ClassificacaoRiscoService service;

    public ClassificacaoRiscoController(ClassificacaoRiscoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OcorrenciaClassificacao> classificar(@PathVariable Long ocorrenciaId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.classificar(ocorrenciaId));
    }

    @GetMapping
    public List<OcorrenciaClassificacao> listar(@PathVariable Long ocorrenciaId) {
        return service.listar(ocorrenciaId);
    }
}
