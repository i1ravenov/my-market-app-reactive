package org.mymarketapp.reactive.repository;

import org.mymarketapp.reactive.model.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
}
