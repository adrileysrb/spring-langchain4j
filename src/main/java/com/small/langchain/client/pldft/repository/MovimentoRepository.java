package com.small.langchain.client.pldft.repository;

import com.small.langchain.client.pldft.model.Movimento;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public class MovimentoRepository extends BaseRepository<Movimento> {

    public MovimentoRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "movimentos", (rs, rowNum) -> new Movimento(
                rs.getLong("id"),
                rs.getLong("pessoa_monitorada_id"),
                rs.getLong("produto_id"),
                rs.getBigDecimal("valor"),
                rs.getTimestamp("data_movimento").toLocalDateTime(),
                rs.getString("tipo_lancamento")
        ));
    }

    public Movimento save(Movimento movimento) {
        Long id = insert(
                "INSERT INTO movimentos (pessoa_monitorada_id, produto_id, valor, data_movimento, tipo_lancamento) " +
                        "VALUES (?, ?, ?, ?, ?)",
                movimento.pessoaMonitoradaId(),
                movimento.produtoId(),
                movimento.valor(),
                Timestamp.valueOf(movimento.dataMovimento()),
                movimento.tipoLancamento()
        );
        return findById(id).orElseThrow();
    }
}
