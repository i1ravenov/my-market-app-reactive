package org.mymarketapp.reactive.repository;

import org.mymarketapp.reactive.model.Item;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ItemRepository extends ReactiveCrudRepository<Item, Long> {

    @Query("SELECT * FROM items ORDER BY id    LIMIT :limit OFFSET :offset")
    Flux<Item> findPage(int limit, long offset);

    @Query("SELECT * FROM items ORDER BY title LIMIT :limit OFFSET :offset")
    Flux<Item> findPageOrderByTitle(int limit, long offset);

    @Query("SELECT * FROM items ORDER BY price LIMIT :limit OFFSET :offset")
    Flux<Item> findPageOrderByPrice(int limit, long offset);

    @Query("SELECT * FROM items WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY id    LIMIT :limit OFFSET :offset")
    Flux<Item> findPageBySearch(String search, int limit, long offset);

    @Query("SELECT * FROM items WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY title LIMIT :limit OFFSET :offset")
    Flux<Item> findPageBySearchOrderByTitle(String search, int limit, long offset);

    @Query("SELECT * FROM items WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY price LIMIT :limit OFFSET :offset")
    Flux<Item> findPageBySearchOrderByPrice(String search, int limit, long offset);

    @Query("SELECT * FROM items WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Flux<Item> findBySearch(String search);

    @Query("SELECT COUNT(*) FROM items WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Mono<Long> countBySearch(String search);
}
