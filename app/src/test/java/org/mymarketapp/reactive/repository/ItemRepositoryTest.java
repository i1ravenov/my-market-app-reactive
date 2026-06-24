package org.mymarketapp.reactive.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mymarketapp.reactive.config.DatabaseConfig;
import org.mymarketapp.reactive.model.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(DatabaseConfig.class)
class ItemRepositoryTest {

    @Autowired ItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        // data.sql seeds 10 items with WHERE NOT EXISTS — idempotent across test runs
    }

    @Test
    void findAll_returnsSeededItems() {
        StepVerifier.create(itemRepository.findAll().collectList())
                .assertNext(items -> assertThat(items).hasSizeGreaterThanOrEqualTo(10))
                .verifyComplete();
    }

    @Test
    void findBySearch_matchesTitle() {
        StepVerifier.create(itemRepository.findBySearch("мяч").collectList())
                .assertNext(items -> {
                    assertThat(items).isNotEmpty();
                    assertThat(items).allMatch(i ->
                            i.getTitle().toLowerCase().contains("мяч")
                                    || i.getDescription().toLowerCase().contains("мяч"));
                })
                .verifyComplete();
    }

    @Test
    void findBySearch_matchesDescription() {
        StepVerifier.create(itemRepository.findBySearch("бассейн").collectList())
                .assertNext(items -> {
                    assertThat(items).isNotEmpty();
                    items.forEach(i ->
                            assertThat(i.getDescription().toLowerCase()).contains("бассейн"));
                })
                .verifyComplete();
    }

    @Test
    void findBySearch_caseInsensitive() {
        StepVerifier.create(itemRepository.findBySearch("МЯЧ").collectList())
                .assertNext(items -> assertThat(items).isNotEmpty())
                .verifyComplete();
    }

    @Test
    void findBySearch_noMatches_returnsEmpty() {
        StepVerifier.create(itemRepository.findBySearch("xyzNotExisting"))
                .verifyComplete();
    }

    @Test
    void countBySearch_matchesExpectedCount() {
        StepVerifier.create(itemRepository.countBySearch("мяч"))
                .assertNext(count -> assertThat(count).isGreaterThanOrEqualTo(1L))
                .verifyComplete();
    }

    @Test
    void countBySearch_noMatches_returnsZero() {
        StepVerifier.create(itemRepository.countBySearch("xyzNotExisting"))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void findById_existingItem_returnsItem() {
        StepVerifier.create(itemRepository.findAll().next()
                        .flatMap(first -> itemRepository.findById(first.getId())))
                .assertNext(item -> assertThat(item).isNotNull())
                .verifyComplete();
    }

    @Test
    void save_persistsNewItem() {
        Item newItem = new Item();
        newItem.setTitle("Тестовый товар");
        newItem.setDescription("Описание");
        newItem.setImgPath("test.svg");
        newItem.setPrice(999L);

        StepVerifier.create(itemRepository.save(newItem)
                        .flatMap(saved -> itemRepository.findById(saved.getId())))
                .assertNext(found -> {
                    assertThat(found.getTitle()).isEqualTo("Тестовый товар");
                    assertThat(found.getPrice()).isEqualTo(999L);
                })
                .verifyComplete();
    }
}
