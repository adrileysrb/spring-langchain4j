package com.small.langchain.client.pldft.enquadramento;

import com.small.langchain.client.pldft.repository.OcorrenciaRepository;
import com.small.langchain.client.pldft.repository.PessoaMonitoradaRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Fonte unica do enquadramento: resolve a pessoa monitorada a partir da ocorrencia e le os
 * campos do cadastro. Tanto as tools expostas a LLM quanto a consulta direta passam por aqui,
 * de modo que as duas abordagens leem exatamente o mesmo dado.
 */
@Service
public class EnquadramentoLookup {

    private final OcorrenciaRepository ocorrenciaRepository;
    private final PessoaMonitoradaRepository pessoaMonitoradaRepository;

    public EnquadramentoLookup(
            OcorrenciaRepository ocorrenciaRepository,
            PessoaMonitoradaRepository pessoaMonitoradaRepository
    ) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.pessoaMonitoradaRepository = pessoaMonitoradaRepository;
    }

    public Optional<Enquadramento> porOcorrencia(long ocorrenciaId) {
        return ocorrenciaRepository.findById(ocorrenciaId)
                .flatMap(ocorrencia -> pessoaMonitoradaRepository.findById(ocorrencia.pessoaMonitoradaId()))
                .map(pessoa -> new Enquadramento(
                        pessoa.nome(),
                        Boolean.TRUE.equals(pessoa.pep()),
                        Boolean.TRUE.equals(pessoa.pem()),
                        Boolean.TRUE.equals(pessoa.funcionario())));
    }
}
