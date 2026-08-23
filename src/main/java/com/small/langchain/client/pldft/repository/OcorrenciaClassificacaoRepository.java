package com.small.langchain.client.pldft.repository;

import com.small.langchain.client.pldft.model.OcorrenciaClassificacao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Repository
public class OcorrenciaClassificacaoRepository extends BaseRepository<OcorrenciaClassificacao> {

    private static final String SEPARADOR = "\n";

    public OcorrenciaClassificacaoRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "ocorrencia_classificacoes", (rs, rowNum) -> new OcorrenciaClassificacao(
                rs.getLong("id"),
                rs.getLong("ocorrencia_id"),
                rs.getString("risco"),
                rs.getInt("score"),
                rs.getString("justificativa"),
                separar(rs.getString("indicadores")),
                rs.getString("modelo"),
                rs.getTimestamp("criado_em").toLocalDateTime()
        ));
    }

    public List<OcorrenciaClassificacao> findByOcorrenciaId(Long ocorrenciaId) {
        return jdbcTemplate.query(
                "SELECT * FROM ocorrencia_classificacoes WHERE ocorrencia_id = ? ORDER BY criado_em DESC",
                rowMapper, ocorrenciaId);
    }

    public OcorrenciaClassificacao save(OcorrenciaClassificacao classificacao) {
        Long id = insert(
                "INSERT INTO ocorrencia_classificacoes " +
                        "(ocorrencia_id, risco, score, justificativa, indicadores, modelo, criado_em) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                classificacao.ocorrenciaId(),
                classificacao.risco(),
                classificacao.score(),
                classificacao.justificativa(),
                juntar(classificacao.indicadores()),
                classificacao.modelo(),
                Timestamp.valueOf(classificacao.criadoEm() != null ? classificacao.criadoEm() : LocalDateTime.now())
        );
        return findById(id).orElseThrow();
    }

    private static List<String> separar(String texto) {
        return texto == null || texto.isBlank() ? List.of() : Arrays.asList(texto.split(SEPARADOR));
    }

    private static String juntar(List<String> indicadores) {
        return indicadores == null ? "" : String.join(SEPARADOR, indicadores);
    }
}
