package org.mymarketapp.reactive.service;

import org.mymarketapp.reactive.dto.ItemDto;
import org.mymarketapp.reactive.dto.OrderDto;
import org.mymarketapp.reactive.model.Order;
import org.mymarketapp.reactive.model.OrderItem;
import org.mymarketapp.reactive.repository.OrderItemRepository;
import org.mymarketapp.reactive.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartService cartService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartService = cartService;
    }

    @Transactional
    public Mono<Long> checkout() {
        return cartService.getCartItems()
                .collectList()
                .flatMap(items -> {
                    if (items.isEmpty()) {
                        return Mono.error(new IllegalStateException("Корзина пуста"));
                    }
                    long total = items.stream()
                            .mapToLong(i -> i.price() * i.count())
                            .sum();

                    Order order = new Order();
                    order.setTotalSum(total);

                    return orderRepository.save(order)
                            .flatMap(saved -> {
                                List<OrderItem> orderItems = items.stream()
                                        .map(dto -> {
                                            OrderItem oi = new OrderItem();
                                            oi.setOrderId(saved.getId());
                                            oi.setItemId(dto.id());
                                            oi.setTitle(dto.title());
                                            oi.setPrice(dto.price());
                                            oi.setCount(dto.count());
                                            return oi;
                                        }).toList();
                                return orderItemRepository.saveAll(orderItems)
                                        .then(cartService.clearCart())
                                        .thenReturn(saved.getId());
                            });
                });
    }

    @Transactional(readOnly = true)
    public Flux<OrderDto> getAllOrders() {
        return orderRepository.findAll()
                .flatMap(this::toDto);
    }

    @Transactional(readOnly = true)
    public Mono<OrderDto> getOrder(long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Заказ не найден: " + id)))
                .flatMap(this::toDto);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Mono<OrderDto> toDto(Order order) {
        return orderItemRepository.findAllByOrderId(order.getId())
                .map(oi -> new ItemDto(oi.getItemId(), oi.getTitle(), null, null, oi.getPrice(), oi.getCount()))
                .collectList()
                .map(items -> new OrderDto(order.getId(), items, order.getTotalSum()));
    }
}
