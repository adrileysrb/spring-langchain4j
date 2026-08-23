package com.small.langchain.client.pldft.repository;

import com.small.langchain.client.pldft.model.AnalisePendencia;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AnalisePendenciaRepository extends BaseRepository<AnalisePendencia> {

    public AnalisePendenciaRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "analise_pendencias", (rs, rowNum) -> new AnalisePendencia(
                rs.getLong("id"),
                rs.getLong("analise_id"),
                rs.getString("descricao"),
                rs.getString("tipo"),
                rs.getString("prioridade"),
                rs.getTimestamp("criado_em").toLocalDateTime()
        ));
    }

    public List<AnalisePendencia> findByAnaliseId(Long analiseId) {
        return jdbcTemplate.query(
                "SELECT * FROM analise_pendencias WHERE analise_id = ? ORDER BY id",
                rowMapper, analiseId);
    }

    public AnalisePendencia save(AnalisePendencia pendencia) {
        Long id = insert(
                "INSERT INTO analise_pendencias (analise_id, descricao, tipo, prioridade, criado_em) " +
                        "VALUES (?, ?, ?, ?, ?)",
                pendencia.analiseId(), pendencia.descricao(), pendencia.tipo(), pendencia.prioridade(),
                Timestamp.valueOf(pendencia.criadoEm() != null ? pendencia.criadoEm() : LocalDateTime.now())
        );
        return findById(id).orElseThrow();
    }

    public int deleteByAnaliseId(Long analiseId) {
        return jdbcTemplate.update("DELETE FROM analise_pendencias WHERE analise_id = ?", analiseId);
    }
}
