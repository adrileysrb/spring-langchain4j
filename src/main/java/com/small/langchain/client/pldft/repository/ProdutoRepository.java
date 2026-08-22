package com.small.langchain.client.pldft.repository;

import com.small.langchain.client.pldft.model.Produto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProdutoRepository extends BaseRepository<Produto> {

    public ProdutoRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "produto", (rs, rowNum) -> new Produto(
                rs.getLong("id"),
                rs.getString("codigo"),
                rs.getString("descricao")
        ));
    }

    public Produto save(Produto produto) {
        Long id = insert(
                "INSERT INTO produto (codigo, descricao) VALUES (?, ?)",
                produto.codigo(), produto.descricao()
        );
        return findById(id).orElseThrow();
    }
}
