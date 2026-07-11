package org.mymarketapp.reactive.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mymarketapp.reactive.model.CartItem;
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
class CartControllerIntegrationTest {

    @Autowired WebTestClient webTestClient;
    @Autowired CartItemRepository cartItemRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
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
    void getCart_emptyCart_returnsPageWithoutTotal() {
        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("Витрина магазина");
                    // total block is hidden by th:if when cart is empty
                    assertThat(body).doesNotContain("Итого:");
                });
    }

    @Test
    void getCart_withItems_showsItemsAndTotal() {
        cartItemRepository.save(new CartItem(1L, 2)).block();
        cartItemRepository.save(new CartItem(2L, 1)).block();

        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("Футбольный мяч");
                    assertThat(body).contains("Теннисная ракетка");
                    // 2*500 + 1*1500 = 2500
                    assertThat(body).contains("2500 руб");
                });
    }

    @Test
    void postCart_plusAction_incrementsCount() {
        cartItemRepository.save(new CartItem(3L, 1)).block();

        webTestClient.post().uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=3&action=PLUS")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("Велосипед"));

        Integer count = cartItemRepository.findById(3L).block().getCount();
        assertThat(count).isEqualTo(2);
    }

    @Test
    void postCart_minusAction_decrementsCount() {
        cartItemRepository.save(new CartItem(3L, 3)).block();

        webTestClient.post().uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=3&action=MINUS")
                .exchange()
                .expectStatus().isOk();

        Integer count = cartItemRepository.findById(3L).block().getCount();
        assertThat(count).isEqualTo(2);
    }

    @Test
    void postCart_deleteAction_removesItem() {
        cartItemRepository.save(new CartItem(4L, 1)).block();

        webTestClient.post().uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=4&action=DELETE")
                .exchange()
                .expectStatus().isOk();

        assertThat(cartItemRepository.findById(4L).block()).isNull();
    }

    @Test
    void postBuy_withItems_createsOrderAndClearsCart() {
        cartItemRepository.save(new CartItem(1L, 2)).block();
        cartItemRepository.save(new CartItem(5L, 1)).block();

        webTestClient.post().uri("/buy")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/orders/\\d+.*");

        assertThat(cartItemRepository.count().block()).isEqualTo(0L);
        assertThat(orderRepository.count().block()).isEqualTo(1L);
        assertThat(orderItemRepository.count().block()).isEqualTo(2L);
    }

    @Test
    void postBuy_emptyCart_returnsServerError() {
        webTestClient.post().uri("/buy")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getCart_insufficientBalance_showsMessageAndHidesButton() {
        when(paymentClient.getBalance()).thenReturn(Mono.just(new BalanceResult(true, 1L)));
        cartItemRepository.save(new CartItem(1L, 2)).block();

        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("Недостаточно средств");
                    assertThat(body).doesNotContain(">Купить<");
                });
    }

    @Test
    void getCart_paymentServiceUnavailable_showsMessageAndHidesButton() {
        when(paymentClient.getBalance()).thenReturn(Mono.just(BalanceResult.unavailable()));
        cartItemRepository.save(new CartItem(1L, 2)).block();

        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("Сервис оплаты недоступен");
                    assertThat(body).doesNotContain(">Купить<");
                });
    }

    @Test
    void postBuy_insufficientBalance_returnsServerErrorAndDoesNotCreateOrder() {
        when(paymentClient.pay(anyLong())).thenReturn(Mono.just(new PaymentResult(true, false, 0L)));
        cartItemRepository.save(new CartItem(1L, 2)).block();

        webTestClient.post().uri("/buy")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("")
                .exchange()
                .expectStatus().is5xxServerError();

        assertThat(orderRepository.count().block()).isEqualTo(0L);
    }
}
