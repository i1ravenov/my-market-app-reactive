package org.mymarketapp.reactive.controller;

import org.mymarketapp.reactive.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Controller
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public Mono<String> listOrders(Model model) {
        return orderService.getAllOrders()
                .collectList()
                .map(orders -> {
                    model.addAttribute("orders", orders);
                    return "orders";
                });
    }

    @GetMapping("/orders/{id}")
    public Mono<String> orderDetail(
            @PathVariable long id,
            @RequestParam(defaultValue = "false") boolean newOrder,
            Model model) {

        return orderService.getOrder(id).map(order -> {
            model.addAttribute("order", order);
            model.addAttribute("newOrder", newOrder);
            return "order";
        });
    }
}
