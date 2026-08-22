package com.small.langchain.client.pldft.repository;

import com.small.langchain.client.pldft.model.PessoaMonitorada;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Repository
public class PessoaMonitoradaRepository extends BaseRepository<PessoaMonitorada> {

    public PessoaMonitoradaRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "pessoa_monitorada", (rs, rowNum) -> new PessoaMonitorada(
                rs.getLong("id"),
                rs.getString("cpf_cnpj"),
                rs.getString("nome"),
                rs.getString("tipo_pessoa"),
                rs.getTimestamp("data_cadastro").toLocalDateTime()
        ));
    }

    public PessoaMonitorada save(PessoaMonitorada pessoa) {
        Long id = insert(
                "INSERT INTO pessoa_monitorada (cpf_cnpj, nome, tipo_pessoa, data_cadastro) VALUES (?, ?, ?, ?)",
                pessoa.cpfCnpj(), pessoa.nome(), pessoa.tipoPessoa(), Timestamp.valueOf(LocalDateTime.now())
        );
        return findById(id).orElseThrow();
    }
}
