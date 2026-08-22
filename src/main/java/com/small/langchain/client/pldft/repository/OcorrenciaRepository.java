package com.small.langchain.client.pldft.repository;

import com.small.langchain.client.pldft.model.Ocorrencia;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class OcorrenciaRepository extends BaseRepository<Ocorrencia> {

    public OcorrenciaRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "ocorrencias", (rs, rowNum) -> new Ocorrencia(
                rs.getLong("id"),
                rs.getLong("pessoa_monitorada_id"),
                rs.getString("status"),
                rs.getTimestamp("data_abertura").toLocalDateTime(),
                rs.getTimestamp("data_encerramento") != null
                        ? rs.getTimestamp("data_encerramento").toLocalDateTime()
                        : null
        ));
    }

    public Ocorrencia save(Ocorrencia ocorrencia) {
        Timestamp dataEncerramento = ocorrencia.dataEncerramento() != null
                ? Timestamp.valueOf(ocorrencia.dataEncerramento())
                : null;
        Long id = insert(
                "INSERT INTO ocorrencias (pessoa_monitorada_id, status, data_abertura, data_encerramento) " +
                        "VALUES (?, ?, ?, ?)",
                ocorrencia.pessoaMonitoradaId(),
                ocorrencia.status(),
                Timestamp.valueOf(LocalDateTime.now()),
                dataEncerramento
        );
        return findById(id).orElseThrow();
    }

    public Optional<Ocorrencia> update(Long id, Ocorrencia ocorrencia) {
        Timestamp dataEncerramento = ocorrencia.dataEncerramento() != null
                ? Timestamp.valueOf(ocorrencia.dataEncerramento())
                : null;
        int rows = jdbcTemplate.update(
                "UPDATE ocorrencias SET pessoa_monitorada_id = ?, status = ?, data_encerramento = ? WHERE id = ?",
                ocorrencia.pessoaMonitoradaId(), ocorrencia.status(), dataEncerramento, id
        );
        return rows == 0 ? Optional.empty() : findById(id);
    }
}
