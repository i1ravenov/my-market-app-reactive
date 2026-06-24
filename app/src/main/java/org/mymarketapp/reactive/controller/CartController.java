package org.mymarketapp.reactive.controller;

import org.mymarketapp.reactive.dto.ActionType;
import org.mymarketapp.reactive.service.CartService;
import org.mymarketapp.reactive.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;

    public CartController(CartService cartService, OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
    }

    @GetMapping("/cart/items")
    public Mono<String> cartPage(Model model) {
        return Mono.zip(
                cartService.getCartItems().collectList(),
                cartService.getTotalSum()
        ).map(t -> {
            model.addAttribute("items", t.getT1());
            model.addAttribute("total", t.getT2());
            return "cart";
        });
    }

    @PostMapping("/cart/items")
    public Mono<String> changeCart(ServerWebExchange exchange, Model model) {
        return exchange.getFormData().flatMap(form -> {
            long id = Long.parseLong(form.getFirst("id"));
            ActionType action = ActionType.valueOf(form.getFirst("action"));
            return cartService.changeCount(id, action)
                    .then(Mono.zip(
                            cartService.getCartItems().collectList(),
                            cartService.getTotalSum()
                    )).map(t -> {
                        model.addAttribute("items", t.getT1());
                        model.addAttribute("total", t.getT2());
                        return "cart";
                    });
        });
    }

    @PostMapping("/buy")
    public Mono<String> buy() {
        return orderService.checkout()
                .map(orderId -> "redirect:/orders/" + orderId + "?newOrder=true");
    }
}
