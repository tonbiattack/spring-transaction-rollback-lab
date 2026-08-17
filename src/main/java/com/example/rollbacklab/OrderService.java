package com.example.rollbacklab;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final JdbcTemplate jdbcTemplate;

    public OrderService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void createOrderThenReject(String orderId) throws OrderRejectedException {
        jdbcTemplate.update("insert into orders (id, status) values (?, ?)", orderId, "CREATED");
        throw new OrderRejectedException("在庫確認に失敗したため注文を拒否しました");
    }
}
