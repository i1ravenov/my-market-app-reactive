package org.mymarketapp.reactive.controller;

import org.mymarketapp.reactive.dto.ActionType;
import org.mymarketapp.reactive.dto.SortType;
import org.mymarketapp.reactive.service.CartService;
import org.mymarketapp.reactive.service.ItemService;
import org.mymarketapp.reactive.util.GridUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
public class ItemController {

    private final ItemService itemService;
    private final CartService cartService;

    private static final int DEFAULT_COLS_N = 3;

    public ItemController(ItemService itemService, CartService cartService) {
        this.itemService = itemService;
        this.cartService = cartService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/items";
    }

    @GetMapping("/items")
    public Mono<String> listItems(
            @RequestParam(defaultValue = "")    String search,
            @RequestParam(defaultValue = "NO")  SortType sort,
            @RequestParam(defaultValue = "1")   int pageNumber,
            @RequestParam(defaultValue = "5")   int pageSize,
            Model model) {

        return Mono.zip(
                itemService.getItemsPage(search, sort, pageNumber, pageSize),
                itemService.buildPageDto(search, sort, pageNumber, pageSize)
        ).map(t -> {
            model.addAttribute("items", GridUtils.splitIntoRows(t.getT1(), DEFAULT_COLS_N));
            model.addAttribute("search", search);
            model.addAttribute("sort", sort.name());
            model.addAttribute("paging", t.getT2());
            return "items";
        });
    }

    @PostMapping("/items")
    public Mono<String> changeCartFromList(ServerWebExchange exchange) {
        return exchange.getFormData().flatMap(form -> {
            long id         = Long.parseLong(form.getFirst("id"));
            ActionType action = ActionType.valueOf(form.getFirst("action"));
            String search   = form.getFirst("search") != null ? form.getFirst("search") : "";
            String sort     = form.getFirst("sort") != null ? form.getFirst("sort") : "NO";
            String pageNum  = form.getFirst("pageNumber") != null ? form.getFirst("pageNumber") : "1";
            String pageSize = form.getFirst("pageSize") != null ? form.getFirst("pageSize") : "5";

            return cartService.changeCount(id, action)
                    .thenReturn("redirect:/items?search=" + search
                            + "&sort=" + sort
                            + "&pageNumber=" + pageNum
                            + "&pageSize=" + pageSize);
        });
    }

    @GetMapping("/items/{id}")
    public Mono<String> itemDetail(@PathVariable long id, Model model) {
        return itemService.getItem(id).map(item -> {
            model.addAttribute("item", item);
            return "item";
        });
    }

    @PostMapping("/items/{id}")
    public Mono<String> changeCartFromItem(
            @PathVariable long id,
            ServerWebExchange exchange,
            Model model) {

        return exchange.getFormData().flatMap(form -> {
            ActionType action = ActionType.valueOf(form.getFirst("action"));
            return cartService.changeCount(id, action)
                    .then(itemService.getItem(id))
                    .map(item -> {
                        model.addAttribute("item", item);
                        return "item";
                    });
        });
    }
}
