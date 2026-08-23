package com.small.langchain.client.llm.tool;

import com.small.langchain.client.pldft.model.PessoaMonitorada;
import com.small.langchain.client.pldft.repository.OcorrenciaRepository;
import com.small.langchain.client.pldft.repository.PessoaMonitoradaRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Resolve a pessoa monitorada de uma ocorrencia de PLDFT. Compartilhado pelas tools de
 * PEP, PEM e funcionario para nao duplicar o mesmo lookup em cada uma.
 */
@Service
class PessoaMonitoradaLookupService {

    private final OcorrenciaRepository ocorrenciaRepository;
    private final PessoaMonitoradaRepository pessoaMonitoradaRepository;

    PessoaMonitoradaLookupService(
            OcorrenciaRepository ocorrenciaRepository,
            PessoaMonitoradaRepository pessoaMonitoradaRepository
    ) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.pessoaMonitoradaRepository = pessoaMonitoradaRepository;
    }

    Optional<PessoaMonitorada> porOcorrencia(long ocorrenciaId) {
        return ocorrenciaRepository.findById(ocorrenciaId)
                .flatMap(ocorrencia -> pessoaMonitoradaRepository.findById(ocorrencia.pessoaMonitoradaId()));
    }

    static String textoBooleano(Boolean valor) {
        return Boolean.TRUE.equals(valor) ? "sim" : "não";
    }
}
