package org.mymarketapp.reactive.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mymarketapp.reactive.config.DatabaseConfig;
import org.mymarketapp.reactive.model.Order;
import org.mymarketapp.reactive.model.OrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(DatabaseConfig.class)
class OrderItemRepositoryTest {

    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;

    private Long orderId;

    @BeforeEach
    void setUp() {
        orderId = orderItemRepository.deleteAll()
                .then(orderRepository.deleteAll())
                .then(saveOrder())
                .block();
    }

    @Test
    void findAllByOrderId_returnsItemsForGivenOrder() {
        StepVerifier.create(orderItemRepository.findAllByOrderId(orderId).collectList())
                .assertNext(items -> {
                    assertThat(items).hasSize(2);
                    assertThat(items).extracting(OrderItem::getTitle)
                            .containsExactlyInAnyOrder("Мяч", "Ракетка");
                })
                .verifyComplete();
    }

    @Test
    void findAllByOrderId_wrongId_returnsEmpty() {
        StepVerifier.create(orderItemRepository.findAllByOrderId(999L))
                .verifyComplete();
    }

    @Test
    void findAllByOrderId_correctlyMapsAllFields() {
        StepVerifier.create(orderItemRepository.findAllByOrderId(orderId)
                        .filter(oi -> "Мяч".equals(oi.getTitle()))
                        .next())
                .assertNext(oi -> {
                    assertThat(oi.getOrderId()).isEqualTo(orderId);
                    assertThat(oi.getItemId()).isEqualTo(1L);
                    assertThat(oi.getPrice()).isEqualTo(500L);
                    assertThat(oi.getCount()).isEqualTo(2);
                })
                .verifyComplete();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Mono<Long> saveOrder() {
        Order order = new Order();
        order.setTotalSum(2500L);

        return orderRepository.save(order).flatMap(saved -> {
            OrderItem oi1 = orderItemOf(saved.getId(), 1L, "Мяч", 500L, 2);
            OrderItem oi2 = orderItemOf(saved.getId(), 2L, "Ракетка", 1500L, 1);
            return orderItemRepository.saveAll(java.util.List.of(oi1, oi2))
                    .then(Mono.just(saved.getId()));
        });
    }

    private OrderItem orderItemOf(long orderId, long itemId, String title, long price, int count) {
        OrderItem oi = new OrderItem();
        oi.setOrderId(orderId);
        oi.setItemId(itemId);
        oi.setTitle(title);
        oi.setPrice(price);
        oi.setCount(count);
        return oi;
    }
}
