package com.small.langchain.client.pldft.repository;

import com.small.langchain.client.pldft.model.Alerta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

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
}
