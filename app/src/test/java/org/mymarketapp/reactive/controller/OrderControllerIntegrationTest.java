package org.mymarketapp.reactive.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mymarketapp.reactive.model.CartItem;
import org.mymarketapp.reactive.model.Order;
import org.mymarketapp.reactive.model.OrderItem;
import org.mymarketapp.reactive.payment.BalanceResult;
import org.mymarketapp.reactive.payment.PaymentClient;
import org.mymarketapp.reactive.payment.PaymentResult;
import org.mymarketapp.reactive.repository.CartItemRepository;
import org.mymarketapp.reactive.repository.OrderItemRepository;
import org.mymarketapp.reactive.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
class OrderControllerIntegrationTest {

    @Autowired WebTestClient webTestClient;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired CartItemRepository cartItemRepository;
    @MockitoBean PaymentClient paymentClient;

    @BeforeEach
    void cleanState() {
        orderItemRepository.deleteAll()
                .then(orderRepository.deleteAll())
                .then(cartItemRepository.deleteAll())
                .block();

        when(paymentClient.getBalance()).thenReturn(Mono.just(new BalanceResult(true, 1_000_000L)));
        when(paymentClient.pay(anyLong())).thenReturn(Mono.just(new PaymentResult(true, true, 1_000_000L)));
    }

    @Test
    void getOrders_empty_returnsPageWithNoOrders() {
        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("Витрина магазина");
                    // no order cards rendered when list is empty
                    assertThat(body).doesNotContain("Заказ №");
                });
    }

    @Test
    void getOrders_withExistingOrders_showsOrdersInList() {
        createOrderWith1500Total();

        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("1500 руб");
                });
    }

    @Test
    void getOrderDetail_existingOrder_showsItemsAndTotal() {
        Long orderId = createOrderWith1500Total();

        webTestClient.get().uri("/orders/" + orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("Теннисная ракетка");
                    assertThat(body).contains("1500 руб");
                });
    }

    @Test
    void getOrderDetail_nonExistent_returnsError() {
        webTestClient.get().uri("/orders/9999")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getOrderDetail_withNewOrderFlag_returnsOk() {
        Long orderId = createOrderWith1500Total();

        webTestClient.get().uri("/orders/" + orderId + "?newOrder=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("1500 руб"));
    }

    @Test
    void checkout_fullFlow_createsOrderAndRedirects() {
        cartItemRepository.save(new CartItem(1L, 3)).block();  // 3 * 500 = 1500

        webTestClient.post().uri("/buy")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("")
                .exchange()
                .expectStatus().is3xxRedirection();

        Order order = orderRepository.findAll().blockFirst();
        assertThat(order).isNotNull();
        assertThat(order.getTotalSum()).isEqualTo(1500L);

        webTestClient.get().uri("/orders/" + order.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("Футбольный мяч");
                    assertThat(body).contains("1500 руб");
                });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Long createOrderWith1500Total() {
        Order order = new Order();
        order.setTotalSum(1500L);
        Order saved = orderRepository.save(order).block();

        OrderItem oi = new OrderItem();
        oi.setOrderId(saved.getId());
        oi.setItemId(2L);
        oi.setTitle("Теннисная ракетка");
        oi.setPrice(1500L);
        oi.setCount(1);
        orderItemRepository.save(oi).block();

        return saved.getId();
    }
}
