package com.example.rollbacklab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Sql(statements = {
        "drop table if exists orders",
        "create table orders (id varchar(64) primary key, status varchar(32) not null)"
})
class OrderServiceTest {
    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rejectedOrderIsNotPersisted() {
        assertThatThrownBy(() -> orderService.createOrderThenReject("order-1"))
                .isInstanceOf(OrderRejectedException.class);

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from orders where id = ?", Integer.class, "order-1");

        assertThat(count).as("拒否された注文はトランザクション終了後に残らない").isZero();
    }
}
