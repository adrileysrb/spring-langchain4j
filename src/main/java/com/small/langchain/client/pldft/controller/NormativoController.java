package com.small.langchain.client.pldft.controller;

import com.small.langchain.client.llm.rag.TrechoRecuperado;
import com.small.langchain.client.pldft.dto.PerguntaNormativoRequest;
import com.small.langchain.client.pldft.dto.RespostaNormativoDto;
import com.small.langchain.client.pldft.service.NormativoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/normativos")
public class NormativoController {

    private final NormativoService service;

    public NormativoController(NormativoService service) {
        this.service = service;
    }

    /** Busca semantica crua: mostra os trechos e os scores, sem o modelo no meio. */
    @GetMapping("/busca")
    public List<TrechoRecuperado> buscar(@RequestParam String q) {
        return service.buscar(q);
    }

    /** Pergunta respondida com base nos trechos recuperados, devolvendo as fontes junto. */
    @PostMapping("/pergunta")
    public RespostaNormativoDto perguntar(@RequestBody PerguntaNormativoRequest request) {
        return service.perguntar(request.pergunta());
    }

    @PostMapping("/reindexacao")
    public ResponseEntity<Void> reindexar() {
        service.reindexar();
        return ResponseEntity.accepted().build();
    }
}
