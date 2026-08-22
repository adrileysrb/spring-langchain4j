package com.small.langchain.client.pldft.repository;

import com.small.langchain.client.pldft.model.PromptTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PromptTemplateRepository extends BaseRepository<PromptTemplate> {

    public PromptTemplateRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "prompt_templates", (rs, rowNum) -> new PromptTemplate(
                rs.getLong("id"),
                rs.getLong("produto_id"),
                rs.getInt("versao"),
                rs.getString("nome"),
                rs.getString("prompt_sistema"),
                rs.getString("prompt_usuario"),
                rs.getString("modelo"),
                rs.getDouble("temperature"),
                rs.getInt("max_tokens"),
                rs.getBoolean("ativo"),
                rs.getTimestamp("criado_em").toLocalDateTime()
        ));
    }

    public Optional<PromptTemplate> findAtivoByProdutoId(Long produtoId) {
        return jdbcTemplate.query(
                "SELECT * FROM prompt_templates WHERE produto_id = ? AND ativo = TRUE",
                rowMapper, produtoId
        ).stream().findFirst();
    }

    public List<PromptTemplate> findByProdutoId(Long produtoId) {
        return jdbcTemplate.query(
                "SELECT * FROM prompt_templates WHERE produto_id = ? ORDER BY versao DESC",
                rowMapper, produtoId
        );
    }

    @Transactional
    public PromptTemplate criarNovaVersao(PromptTemplate template) {
        jdbcTemplate.update(
                "UPDATE prompt_templates SET ativo = FALSE WHERE produto_id = ? AND ativo = TRUE",
                template.produtoId()
        );

        Integer ultimaVersao = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(versao), 0) FROM prompt_templates WHERE produto_id = ?",
                Integer.class, template.produtoId()
        );
        int proximaVersao = (ultimaVersao == null ? 0 : ultimaVersao) + 1;

        Long id = insert(
                "INSERT INTO prompt_templates " +
                        "(produto_id, versao, nome, prompt_sistema, prompt_usuario, modelo, temperature, max_tokens, ativo, criado_em) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?)",
                template.produtoId(), proximaVersao, template.nome(),
                template.promptSistema(), template.promptUsuario(),
                template.modelo(), template.temperature(), template.maxTokens(),
                Timestamp.valueOf(LocalDateTime.now())
        );
        return findById(id).orElseThrow();
    }
}
