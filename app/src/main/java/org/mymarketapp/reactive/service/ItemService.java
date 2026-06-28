package org.mymarketapp.reactive.service;

import org.mymarketapp.reactive.dto.ItemDto;
import org.mymarketapp.reactive.dto.PageDto;
import org.mymarketapp.reactive.dto.SortType;
import org.mymarketapp.reactive.model.Item;
import org.mymarketapp.reactive.repository.CartItemRepository;
import org.mymarketapp.reactive.repository.ItemRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;

    public ItemService(ItemRepository itemRepository, CartItemRepository cartItemRepository) {
        this.itemRepository = itemRepository;
        this.cartItemRepository = cartItemRepository;
    }

    public Mono<List<List<Optional<ItemDto>>>> getItemsPage(String search, SortType sort, int pageNumber, int pageSize) {
        Mono<Map<Long, Integer>> cartMap = cartItemRepository.findAll()
                .collectMap(ci -> ci.getItemId(), ci -> ci.getCount());

        Mono<List<Item>> items = itemsFiltered(search)
                .collectSortedList(comparator(sort));

        return Mono.zip(items, cartMap).map(t -> {
            List<Item> all = t.getT1();
            Map<Long, Integer> counts = t.getT2();
            int offset = (pageNumber - 1) * pageSize;
            List<ItemDto> flat = all.stream()
                    .skip(offset)
                    .limit(pageSize)
                    .map(item -> toDto(item, counts))
                    .collect(Collectors.toList());
            return splitIntoRows(flat, 3);
        });
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
        Mono<Map<Long, Integer>> cartMap = cartItemRepository.findAll()
                .collectMap(ci -> ci.getItemId(), ci -> ci.getCount());

        return Mono.zip(
                itemRepository.findById(id)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Товар не найден: " + id))),
                cartMap
        ).map(t -> toDto(t.getT1(), t.getT2()));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private reactor.core.publisher.Flux<Item> itemsFiltered(String search) {
        return (search == null || search.isBlank())
                ? itemRepository.findAll()
                : itemRepository.findBySearch(search);
    }

    private Comparator<Item> comparator(SortType sort) {
        if (sort == null) return Comparator.comparing(Item::getId);
        return switch (sort) {
            case ALPHA -> Comparator.comparing(Item::getTitle);
            case PRICE -> Comparator.comparing(Item::getPrice);
            default    -> Comparator.comparing(Item::getId);
        };
    }

    private ItemDto toDto(Item item, Map<Long, Integer> counts) {
        int count = counts.getOrDefault(item.getId(), 0);
        return new ItemDto(item.getId(), item.getTitle(), item.getDescription(),
                item.getImgPath(), item.getPrice(), count);
    }

    private List<List<Optional<ItemDto>>> splitIntoRows(List<ItemDto> flat, int cols) {
        List<List<Optional<ItemDto>>> rows = new ArrayList<>();
        int i = 0;
        while (i < flat.size()) {
            List<Optional<ItemDto>> row = new ArrayList<>();
            for (int c = 0; c < cols; c++) {
                row.add(i < flat.size() ? Optional.of(flat.get(i++)) : Optional.empty());
            }
            rows.add(row);
        }
        return rows;
    }
}
