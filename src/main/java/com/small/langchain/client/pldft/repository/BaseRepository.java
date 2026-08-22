package com.small.langchain.client.pldft.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

public abstract class BaseRepository<T> {

    protected final JdbcTemplate jdbcTemplate;
    private final String tableName;
    protected final RowMapper<T> rowMapper;

    protected BaseRepository(JdbcTemplate jdbcTemplate, String tableName, RowMapper<T> rowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
        this.rowMapper = rowMapper;
    }

    public List<T> findAll() {
        return jdbcTemplate.query("SELECT * FROM " + tableName, rowMapper);
    }

    public Optional<T> findById(Long id) {
        return jdbcTemplate.query("SELECT * FROM " + tableName + " WHERE id = ?", rowMapper, id)
                .stream()
                .findFirst();
    }

    protected Long insert(String sql, Object... params) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public boolean delete(Long id) {
        return jdbcTemplate.update("DELETE FROM " + tableName + " WHERE id = ?", id) > 0;
    }
}
