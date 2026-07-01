package org.mymarketapp.reactive.service;

import org.mymarketapp.reactive.dto.ActionType;
import org.mymarketapp.reactive.dto.ItemDto;
import org.mymarketapp.reactive.exception.ItemNotFoundException;
import org.mymarketapp.reactive.model.CartItem;
import org.mymarketapp.reactive.repository.CartItemRepository;
import org.mymarketapp.reactive.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    public CartService(CartItemRepository cartItemRepository, ItemRepository itemRepository) {
        this.cartItemRepository = cartItemRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public Mono<Void> changeCount(long itemId, ActionType action) {
        if (action == ActionType.DELETE) {
            return cartItemRepository.deleteById(itemId);
        }

        return cartItemRepository.findById(itemId)
                .flatMap(ci -> {
                    if (action == ActionType.PLUS) {
                        ci.setCount(ci.getCount() + 1);
                        return cartItemRepository.save(ci);
                    } else {
                        int newCount = ci.getCount() - 1;
                        if (newCount <= 0) {
                            return cartItemRepository.deleteById(itemId).then(Mono.just(ci));
                        } else {
                            ci.setCount(newCount);
                            return cartItemRepository.save(ci);
                        }
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    if (action == ActionType.PLUS) {
                        return cartItemRepository.save(new CartItem(itemId, 1));
                    }
                    return Mono.empty();
                }))
                .then();
    }

    @Transactional(readOnly = true)
    public Flux<ItemDto> getCartItems() {
        return cartItemRepository.findAll()
                .filter(ci -> ci.getCount() > 0)
                .flatMap(ci -> itemRepository.findById(ci.getItemId())
                        .switchIfEmpty(Mono.error(new ItemNotFoundException("The product with id = " + ci.getItemId() + " is not found")))
                        .map(item -> new ItemDto(item.getId(), item.getTitle(), item.getDescription(),
                                item.getImgPath(), item.getPrice(), ci.getCount())));
    }

    @Transactional(readOnly = true)
    public Mono<Long> getTotalSum() {
        return getCartItems()
                .map(item -> item.price() * item.count())
                .reduce(0L, Long::sum);
    }

    @Transactional
    public Mono<Void> clearCart() {
        return cartItemRepository.deleteAll();
    }
}
