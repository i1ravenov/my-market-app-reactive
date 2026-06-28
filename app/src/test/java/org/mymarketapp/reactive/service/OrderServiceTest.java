package org.mymarketapp.reactive.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mymarketapp.reactive.dto.ItemDto;
import org.mymarketapp.reactive.exception.BasketIsEmptyException;
import org.mymarketapp.reactive.exception.OrderNotFoundException;
import org.mymarketapp.reactive.model.Order;
import org.mymarketapp.reactive.model.OrderItem;
import org.mymarketapp.reactive.repository.OrderItemRepository;
import org.mymarketapp.reactive.repository.OrderRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;
    @Mock
    OrderItemRepository orderItemRepository;
    @Mock
    CartService cartService;
    @InjectMocks
    OrderService orderService;

    // ── checkout ──────────────────────────────────────────────────────────────

    @Test
    void checkout_success_savesOrderAndClearsCart() {
        List<ItemDto> cartItems = List.of(
                new ItemDto(1L, "Мяч", null, null, 500L, 2),
                new ItemDto(2L, "Ракетка", null, null, 1500L, 1));

        when(cartService.getCartItems()).thenReturn(Flux.fromIterable(cartItems));

        Order saved = new Order();
        saved.setId(10L);
        saved.setTotalSum(2500L);
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(saved));
        when(orderItemRepository.saveAll(anyIterable())).thenReturn(Flux.empty());
        when(cartService.clearCart()).thenReturn(Mono.empty());

        StepVerifier.create(orderService.checkout())
                .expectNext(10L)
                .verifyComplete();

        verify(orderRepository).save(argThat(o -> o.getTotalSum() == 2500L));
        verify(orderItemRepository).saveAll(argThat((Iterable<OrderItem> items) -> {
            List<OrderItem> list = (List<OrderItem>) items;
            return list.size() == 2
                    && list.get(0).getItemId().equals(1L)
                    && list.get(0).getCount() == 2
                    && list.get(1).getItemId().equals(2L);
        }));
        verify(cartService).clearCart();
    }

    @Test
    void checkout_emptyCart_emitsError() {
        when(cartService.getCartItems()).thenReturn(Flux.empty());

        StepVerifier.create(orderService.checkout())
                .expectErrorMatches(ex -> ex instanceof BasketIsEmptyException
                        && ex.getMessage().contains("empty"))
                .verify();

        verify(orderRepository, never()).save(any());
    }

    // ── getAllOrders ──────────────────────────────────────────────────────────

    @Test
    void getAllOrders_returnsOrdersWithItems() {
        Order order1 = orderWith(1L, 1000L);
        Order order2 = orderWith(2L, 500L);
        when(orderRepository.findAll()).thenReturn(Flux.just(order1, order2));
        when(orderItemRepository.findAllByOrderId(1L))
                .thenReturn(Flux.just(orderItem(1L, "Мяч", 500L, 2)));
        when(orderItemRepository.findAllByOrderId(2L))
                .thenReturn(Flux.just(orderItem(2L, "Скакалка", 500L, 1)));

        StepVerifier.create(orderService.getAllOrders())
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(1L);
                    assertThat(dto.totalSum()).isEqualTo(1000L);
                    assertThat(dto.items()).hasSize(1);
                    assertThat(dto.items().get(0).title()).isEqualTo("Мяч");
                })
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(2L);
                    assertThat(dto.items().get(0).title()).isEqualTo("Скакалка");
                })
                .verifyComplete();
    }

    @Test
    void getAllOrders_emptyRepository_returnsEmpty() {
        when(orderRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(orderService.getAllOrders())
                .verifyComplete();
    }

    // ── getOrder ─────────────────────────────────────────────────────────────

    @Test
    void getOrder_found_returnsOrderDto() {
        Order order = orderWith(5L, 3000L);
        when(orderRepository.findById(5L)).thenReturn(Mono.just(order));
        when(orderItemRepository.findAllByOrderId(5L))
                .thenReturn(Flux.just(orderItem(1L, "Велосипед", 3000L, 1)));

        StepVerifier.create(orderService.getOrder(5L))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(5L);
                    assertThat(dto.totalSum()).isEqualTo(3000L);
                    assertThat(dto.items()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    void getOrder_notFound_emitsError() {
        when(orderRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.getOrder(99L))
                .expectErrorMatches(ex -> ex instanceof OrderNotFoundException
                        && ex.getMessage().contains("99"))
                .verify();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Order orderWith(long id, long totalSum) {
        Order o = new Order();
        o.setId(id);
        o.setTotalSum(totalSum);
        return o;
    }

    private OrderItem orderItem(long itemId, String title, long price, int count) {
        OrderItem oi = new OrderItem();
        oi.setItemId(itemId);
        oi.setTitle(title);
        oi.setPrice(price);
        oi.setCount(count);
        return oi;
    }
}
