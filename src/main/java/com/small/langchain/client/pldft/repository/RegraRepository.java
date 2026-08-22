package com.small.langchain.client.pldft.repository;

import com.small.langchain.client.pldft.model.Regra;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RegraRepository extends BaseRepository<Regra> {

    public RegraRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "regras", (rs, rowNum) -> new Regra(
                rs.getLong("id"),
                rs.getString("codigo"),
                rs.getString("descricao"),
                rs.getBoolean("ativo")
        ));
    }

    public Regra save(Regra regra) {
        Long id = insert(
                "INSERT INTO regras (codigo, descricao, ativo) VALUES (?, ?, ?)",
                regra.codigo(), regra.descricao(), regra.ativo() != null ? regra.ativo() : true
        );
        return findById(id).orElseThrow();
    }

    public Optional<Regra> update(Long id, Regra regra) {
        int rows = jdbcTemplate.update(
                "UPDATE regras SET codigo = ?, descricao = ?, ativo = ? WHERE id = ?",
                regra.codigo(), regra.descricao(), regra.ativo() != null ? regra.ativo() : true, id
        );
        return rows == 0 ? Optional.empty() : findById(id);
    }
}
