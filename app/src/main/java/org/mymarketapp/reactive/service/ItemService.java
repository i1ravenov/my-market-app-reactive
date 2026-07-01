package org.mymarketapp.reactive.service;

import org.mymarketapp.reactive.dto.ItemDto;
import org.mymarketapp.reactive.dto.PageDto;
import org.mymarketapp.reactive.dto.SortType;
import org.mymarketapp.reactive.exception.ItemNotFoundException;
import org.mymarketapp.reactive.model.Item;
import org.mymarketapp.reactive.repository.CartItemRepository;
import org.mymarketapp.reactive.repository.ItemRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;

    public ItemService(ItemRepository itemRepository, CartItemRepository cartItemRepository) {
        this.itemRepository = itemRepository;
        this.cartItemRepository = cartItemRepository;
    }

    public Mono<List<ItemDto>> getItemsPage(String search, SortType sort, int pageNumber, int pageSize) {
        Mono<Map<Long, Integer>> cartMap = cartItemRepository.findAll()
                .collectMap(ci -> ci.getItemId(), ci -> ci.getCount());

        long offset = (long) (pageNumber - 1) * pageSize;
        Mono<List<Item>> items = itemsPage(search, sort, pageSize, offset).collectList();

        return Mono.zip(items, cartMap).map(t ->
                t.getT1().stream()
                        .map(item -> toDto(item, t.getT2()))
                        .collect(Collectors.toList()));
    }

    public Mono<PageDto> buildPageDto(String search, SortType sort, int pageNumber, int pageSize) {
        Mono<Long> total = (search == null || search.isBlank())
                ? itemRepository.count()
                : itemRepository.countBySearch(search);

        return total.map(count -> {
            boolean hasPrev = pageNumber > 1;
            boolean hasNext = (long) pageNumber * pageSize < count;
            return new PageDto(pageSize, pageNumber, hasPrev, hasNext);
        });
    }

    public Mono<ItemDto> getItem(long id) {
        Mono<Item> item = itemRepository.findById(id)
                .switchIfEmpty(Mono.error(new ItemNotFoundException("The product with id = " + id + " is not found")));

        Mono<Integer> count = cartItemRepository.findById(id)
                .map(ci -> ci.getCount())
                .defaultIfEmpty(0);

        return Mono.zip(item, count)
                .map(t -> {
                    Item i = t.getT1();
                    return new ItemDto(i.getId(), i.getTitle(), i.getDescription(), i.getImgPath(), i.getPrice(), t.getT2());
                });
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Flux<Item> itemsPage(String search, SortType sort, int limit, long offset) {
        boolean hasSearch = search != null && !search.isBlank();
        SortType effective = sort != null ? sort : SortType.NO;
        return switch (effective) {
            case ALPHA -> hasSearch
                    ? itemRepository.findPageBySearchOrderByTitle(search, limit, offset)
                    : itemRepository.findPageOrderByTitle(limit, offset);
            case PRICE -> hasSearch
                    ? itemRepository.findPageBySearchOrderByPrice(search, limit, offset)
                    : itemRepository.findPageOrderByPrice(limit, offset);
            default -> hasSearch
                    ? itemRepository.findPageBySearch(search, limit, offset)
                    : itemRepository.findPage(limit, offset);
        };
    }

    private ItemDto toDto(Item item, Map<Long, Integer> counts) {
        int count = counts.getOrDefault(item.getId(), 0);
        return new ItemDto(item.getId(), item.getTitle(), item.getDescription(),
                item.getImgPath(), item.getPrice(), count);
    }
}
