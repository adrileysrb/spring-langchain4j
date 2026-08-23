package com.small.langchain.client.llm.observability;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class LlmCallRepository {

    private static final RowMapper<LlmCall> ROW_MAPPER = (rs, rowNum) -> new LlmCall(
            rs.getLong("id"),
            rs.getString("tarefa"),
            rs.getString("modelo"),
            (Integer) rs.getObject("tokens_entrada"),
            (Integer) rs.getObject("tokens_saida"),
            (Integer) rs.getObject("tokens_total"),
            (Long) rs.getObject("duracao_ms"),
            rs.getString("finish_reason"),
            rs.getBoolean("sucesso"),
            rs.getString("erro"),
            rs.getTimestamp("criado_em").toLocalDateTime()
    );

    private static final RowMapper<LlmCallStats> STATS_ROW_MAPPER = (rs, rowNum) -> new LlmCallStats(
            rs.getString("tarefa"),
            rs.getLong("chamadas"),
            rs.getLong("erros"),
            rs.getLong("tokens_entrada"),
            rs.getLong("tokens_saida"),
            rs.getLong("tokens_total"),
            (Long) rs.getObject("duracao_media_ms")
    );

    private final JdbcTemplate jdbcTemplate;

    public LlmCallRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(LlmCall call) {
        jdbcTemplate.update(
                "INSERT INTO llm_chamadas (tarefa, modelo, tokens_entrada, tokens_saida, tokens_total, " +
                        "duracao_ms, finish_reason, sucesso, erro, criado_em) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                call.tarefa(), call.modelo(), call.tokensEntrada(), call.tokensSaida(), call.tokensTotal(),
                call.duracaoMs(), call.finishReason(), call.sucesso(), call.erro(),
                Timestamp.valueOf(call.criadoEm() != null ? call.criadoEm() : LocalDateTime.now())
        );
    }

    public List<LlmCall> ultimas(int limite) {
        return jdbcTemplate.query(
                "SELECT * FROM llm_chamadas ORDER BY id DESC LIMIT ?", ROW_MAPPER, limite);
    }

    public List<LlmCallStats> estatisticasPorTarefa() {
        return jdbcTemplate.query(
                "SELECT tarefa, " +
                        "  COUNT(*) AS chamadas, " +
                        "  SUM(CASE WHEN sucesso THEN 0 ELSE 1 END) AS erros, " +
                        "  COALESCE(SUM(tokens_entrada), 0) AS tokens_entrada, " +
                        "  COALESCE(SUM(tokens_saida), 0) AS tokens_saida, " +
                        "  COALESCE(SUM(tokens_total), 0) AS tokens_total, " +
                        "  CAST(AVG(duracao_ms) AS BIGINT) AS duracao_media_ms " +
                        "FROM llm_chamadas GROUP BY tarefa ORDER BY chamadas DESC",
                STATS_ROW_MAPPER);
    }
}
