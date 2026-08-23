package com.small.langchain.client.pldft.repository;

import com.small.langchain.client.pldft.model.Analise;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AnaliseRepository extends BaseRepository<Analise> {

    public AnaliseRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "analises", (rs, rowNum) -> new Analise(
                rs.getLong("id"),
                rs.getLong("ocorrencia_id"),
                rs.getString("analista"),
                rs.getTimestamp("data_analise").toLocalDateTime(),
                rs.getString("parecer")
        ));
    }

    public List<Analise> findByOcorrenciaId(Long ocorrenciaId) {
        return jdbcTemplate.query(
                "SELECT * FROM analises WHERE ocorrencia_id = ? ORDER BY data_analise DESC",
                rowMapper, ocorrenciaId);
    }

    public Analise save(Analise analise) {
        Long id = insert(
                "INSERT INTO analises (ocorrencia_id, analista, data_analise, parecer) VALUES (?, ?, ?, ?)",
                analise.ocorrenciaId(),
                analise.analista(),
                Timestamp.valueOf(LocalDateTime.now()),
                analise.parecer()
        );
        return findById(id).orElseThrow();
    }

    public Optional<Analise> update(Long id, Analise analise) {
        int rows = jdbcTemplate.update(
                "UPDATE analises SET analista = ?, parecer = ? WHERE id = ?",
                analise.analista(), analise.parecer(), id
        );
        return rows == 0 ? Optional.empty() : findById(id);
    }
}
