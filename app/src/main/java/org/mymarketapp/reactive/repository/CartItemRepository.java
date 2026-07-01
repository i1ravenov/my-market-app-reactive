package org.mymarketapp.reactive.repository;

import org.mymarketapp.reactive.model.CartItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long> {
}
