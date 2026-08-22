package com.small.langchain.client.pldft.model;

import java.time.LocalDateTime;

public record PessoaMonitorada(
        Long id,
        String cpfCnpj,
        String nome,
        String tipoPessoa,
        Boolean pep,
        Boolean pem,
        Boolean funcionario,
        LocalDateTime dataCadastro
) {
}
