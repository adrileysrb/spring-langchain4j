package com.small.langchain.client.pldft.controller;

import com.small.langchain.client.pldft.dto.PerguntaAssistenteRequest;
import com.small.langchain.client.pldft.dto.RespostaAssistenteDto;
import com.small.langchain.client.pldft.service.AssistenteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ocorrencias/{ocorrenciaId}/assistente")
public class AssistenteController {

    private final AssistenteService service;

    public AssistenteController(AssistenteService service) {
        this.service = service;
    }

    @PostMapping
    public RespostaAssistenteDto perguntar(
            @PathVariable Long ocorrenciaId,
            @RequestBody PerguntaAssistenteRequest request
    ) {
        return service.perguntar(ocorrenciaId, request.pergunta());
    }

    /** Descarta a memoria da conversa desta ocorrencia. */
    @DeleteMapping
    public ResponseEntity<Void> limpar(@PathVariable Long ocorrenciaId) {
        service.limpar(ocorrenciaId);
        return ResponseEntity.noContent().build();
    }
}
