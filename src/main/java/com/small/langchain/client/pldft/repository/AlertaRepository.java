package com.small.langchain.client.pldft.repository;

import com.small.langchain.client.pldft.model.Alerta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class AlertaRepository extends BaseRepository<Alerta> {

    public AlertaRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "alertas", (rs, rowNum) -> new Alerta(
                rs.getLong("id"),
                rs.getLong("pessoa_monitorada_id"),
                rs.getLong("regra_id"),
                rs.getTimestamp("data_geracao").toLocalDateTime(),
                (Long) rs.getObject("ocorrencia_id")
        ));
    }

    public Alerta save(Alerta alerta) {
        Long id = insert(
                "INSERT INTO alertas (pessoa_monitorada_id, regra_id, data_geracao, ocorrencia_id) VALUES (?, ?, ?, ?)",
                alerta.pessoaMonitoradaId(),
                alerta.regraId(),
                Timestamp.valueOf(LocalDateTime.now()),
                alerta.ocorrenciaId()
        );
        return findById(id).orElseThrow();
    }

    public Optional<Alerta> update(Long id, Alerta alerta) {
        int rows = jdbcTemplate.update(
                "UPDATE alertas SET pessoa_monitorada_id = ?, regra_id = ?, ocorrencia_id = ? WHERE id = ?",
                alerta.pessoaMonitoradaId(), alerta.regraId(), alerta.ocorrenciaId(), id
        );
        return rows == 0 ? Optional.empty() : findById(id);
    }
}
