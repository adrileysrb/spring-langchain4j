package com.small.langchain.client.pldft.repository;

import com.small.langchain.client.pldft.model.PessoaMonitorada;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class PessoaMonitoradaRepository extends BaseRepository<PessoaMonitorada> {

    public PessoaMonitoradaRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "pessoa_monitorada", (rs, rowNum) -> new PessoaMonitorada(
                rs.getLong("id"),
                rs.getString("cpf_cnpj"),
                rs.getString("nome"),
                rs.getString("tipo_pessoa"),
                rs.getBoolean("pep"),
                rs.getBoolean("pem"),
                rs.getBoolean("funcionario"),
                rs.getTimestamp("data_cadastro").toLocalDateTime()
        ));
    }

    public PessoaMonitorada save(PessoaMonitorada pessoa) {
        Long id = insert(
                "INSERT INTO pessoa_monitorada (cpf_cnpj, nome, tipo_pessoa, pep, pem, funcionario, data_cadastro) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                pessoa.cpfCnpj(), pessoa.nome(), pessoa.tipoPessoa(),
                Boolean.TRUE.equals(pessoa.pep()), Boolean.TRUE.equals(pessoa.pem()), Boolean.TRUE.equals(pessoa.funcionario()),
                Timestamp.valueOf(LocalDateTime.now())
        );
        return findById(id).orElseThrow();
    }

    public Optional<PessoaMonitorada> update(Long id, PessoaMonitorada pessoa) {
        int rows = jdbcTemplate.update(
                "UPDATE pessoa_monitorada SET cpf_cnpj = ?, nome = ?, tipo_pessoa = ?, pep = ?, pem = ?, funcionario = ? WHERE id = ?",
                pessoa.cpfCnpj(), pessoa.nome(), pessoa.tipoPessoa(),
                Boolean.TRUE.equals(pessoa.pep()), Boolean.TRUE.equals(pessoa.pem()), Boolean.TRUE.equals(pessoa.funcionario()),
                id
        );
        return rows == 0 ? Optional.empty() : findById(id);
    }
}
