package com.small.langchain.client.pldft.repository;

import com.small.langchain.client.pldft.model.AnaliseMelhoria;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class AnaliseMelhoriaRepository extends BaseRepository<AnaliseMelhoria> {

    public AnaliseMelhoriaRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "analise_melhorias", (rs, rowNum) -> new AnaliseMelhoria(
                rs.getLong("id"),
                rs.getLong("analise_id"),
                rs.getLong("prompt_template_id"),
                rs.getString("texto_original"),
                rs.getString("texto_sugerido"),
                rs.getString("status"),
                rs.getString("erro_mensagem"),
                rs.getTimestamp("solicitado_em").toLocalDateTime(),
                rs.getTimestamp("respondido_em") != null
                        ? rs.getTimestamp("respondido_em").toLocalDateTime()
                        : null
        ));
    }

    public List<AnaliseMelhoria> findByAnaliseId(Long analiseId) {
        return jdbcTemplate.query(
                "SELECT * FROM analise_melhorias WHERE analise_id = ? ORDER BY solicitado_em DESC",
                rowMapper, analiseId
        );
    }

    public AnaliseMelhoria save(AnaliseMelhoria melhoria) {
        Long id = insert(
                "INSERT INTO analise_melhorias " +
                        "(analise_id, prompt_template_id, texto_original, texto_sugerido, status, erro_mensagem, solicitado_em, respondido_em) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                melhoria.analiseId(), melhoria.promptTemplateId(),
                melhoria.textoOriginal(), melhoria.textoSugerido(),
                melhoria.status(), melhoria.erroMensagem(),
                Timestamp.valueOf(melhoria.solicitadoEm()),
                melhoria.respondidoEm() != null ? Timestamp.valueOf(melhoria.respondidoEm()) : null
        );
        return findById(id).orElseThrow();
    }

    public Optional<AnaliseMelhoria> updateStatus(Long id, String status) {
        int rows = jdbcTemplate.update(
                "UPDATE analise_melhorias SET status = ? WHERE id = ?",
                status, id
        );
        return rows == 0 ? Optional.empty() : findById(id);
    }
}
