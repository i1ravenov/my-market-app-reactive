package org.mymarketapp.reactive.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mymarketapp.reactive.dto.SortType;
import org.mymarketapp.reactive.model.CartItem;
import org.mymarketapp.reactive.model.Item;
import org.mymarketapp.reactive.repository.CartItemRepository;
import org.mymarketapp.reactive.repository.ItemRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    ItemRepository itemRepository;
    @Mock
    CartItemRepository cartItemRepository;
    @InjectMocks
    ItemService itemService;

    // ── getItemsPage ──────────────────────────────────────────────────────────

    @Test
    void getItemsPage_noSearch_returnsPageItems() {
        when(itemRepository.findPage(3, 0L)).thenReturn(Flux.fromIterable(threeItems()));
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(itemService.getItemsPage("", SortType.NO, 1, 3))
                .assertNext(items -> {
                    assertThat(items).hasSize(3);
                    assertThat(items.get(0).title()).isEqualTo("Alpha");
                })
                .verifyComplete();
    }

    @Test
    void getItemsPage_pageSizeTwo_passesCorrectLimitAndOffset() {
        when(itemRepository.findPage(2, 0L)).thenReturn(Flux.just(
                item(1L, "Alpha", 100L), item(2L, "Beta", 200L)));
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(itemService.getItemsPage("", SortType.NO, 1, 2))
                .assertNext(items -> {
                    assertThat(items).hasSize(2);
                    assertThat(items.get(0).title()).isEqualTo("Alpha");
                    assertThat(items.get(1).title()).isEqualTo("Beta");
                })
                .verifyComplete();
    }

    @Test
    void getItemsPage_secondPage_passesCorrectOffset() {
        when(itemRepository.findPage(2, 2L)).thenReturn(Flux.just(item(3L, "Gamma", 300L)));
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(itemService.getItemsPage("", SortType.NO, 2, 2))
                .assertNext(items -> {
                    assertThat(items).hasSize(1);
                    assertThat(items.get(0).title()).isEqualTo("Gamma");
                })
                .verifyComplete();
    }

    @Test
    void getItemsPage_withSearch_callsSearchRepository() {
        when(itemRepository.findPageBySearch("мяч", 5, 0L)).thenReturn(Flux.just(item(1L, "Мяч", 500L)));
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(itemService.getItemsPage("мяч", SortType.NO, 1, 5))
                .assertNext(items -> assertThat(items.get(0).title()).isEqualTo("Мяч"))
                .verifyComplete();
    }

    @Test
    void getItemsPage_sortAlpha_callsOrderByTitleRepository() {
        when(itemRepository.findPageOrderByTitle(5, 0L)).thenReturn(Flux.just(
                item(2L, "Apple", 200L), item(1L, "Zebra", 100L)));
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(itemService.getItemsPage("", SortType.ALPHA, 1, 5))
                .assertNext(items -> {
                    assertThat(items.get(0).title()).isEqualTo("Apple");
                    assertThat(items.get(1).title()).isEqualTo("Zebra");
                })
                .verifyComplete();
    }

    @Test
    void getItemsPage_sortPrice_callsOrderByPriceRepository() {
        when(itemRepository.findPageOrderByPrice(5, 0L)).thenReturn(Flux.just(
                item(2L, "Cheap", 100L), item(1L, "Expensive", 5000L)));
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(itemService.getItemsPage("", SortType.PRICE, 1, 5))
                .assertNext(items -> {
                    assertThat(items.get(0).title()).isEqualTo("Cheap");
                    assertThat(items.get(1).title()).isEqualTo("Expensive");
                })
                .verifyComplete();
    }

    @Test
    void getItemsPage_includesCartCounts() {
        when(itemRepository.findPage(5, 0L)).thenReturn(Flux.just(item(7L, "Мяч", 500L)));
        CartItem ci = new CartItem(7L, 3);
        when(cartItemRepository.findAll()).thenReturn(Flux.just(ci));

        StepVerifier.create(itemService.getItemsPage("", SortType.NO, 1, 5))
                .assertNext(items -> assertThat(items.get(0).count()).isEqualTo(3))
                .verifyComplete();
    }

    // ── buildPageDto ──────────────────────────────────────────────────────────

    @Test
    void buildPageDto_firstPage_noPrevious() {
        when(itemRepository.count()).thenReturn(Mono.just(10L));

        StepVerifier.create(itemService.buildPageDto("", SortType.NO, 1, 5))
                .assertNext(dto -> {
                    assertThat(dto.hasPrevious()).isFalse();
                    assertThat(dto.hasNext()).isTrue();
                    assertThat(dto.pageNumber()).isEqualTo(1);
                    assertThat(dto.pageSize()).isEqualTo(5);
                })
                .verifyComplete();
    }

    @Test
    void buildPageDto_lastPage_noNext() {
        when(itemRepository.count()).thenReturn(Mono.just(10L));

        StepVerifier.create(itemService.buildPageDto("", SortType.NO, 2, 5))
                .assertNext(dto -> {
                    assertThat(dto.hasPrevious()).isTrue();
                    assertThat(dto.hasNext()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void buildPageDto_withSearch_usesSearchCount() {
        when(itemRepository.countBySearch("мяч")).thenReturn(Mono.just(2L));

        StepVerifier.create(itemService.buildPageDto("мяч", SortType.NO, 1, 5))
                .assertNext(dto -> {
                    assertThat(dto.hasPrevious()).isFalse();
                    assertThat(dto.hasNext()).isFalse();
                })
                .verifyComplete();
    }

    // ── getItem ───────────────────────────────────────────────────────────────

    @Test
    void getItem_found_returnsItemDtoWithCartCount() {
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item(1L, "Мяч", 500L)));
        when(cartItemRepository.findAll()).thenReturn(Flux.just(new CartItem(1L, 2)));

        StepVerifier.create(itemService.getItem(1L))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(1L);
                    assertThat(dto.title()).isEqualTo("Мяч");
                    assertThat(dto.price()).isEqualTo(500L);
                    assertThat(dto.count()).isEqualTo(2);
                })
                .verifyComplete();
    }

    @Test
    void getItem_notFound_emitsError() {
        when(itemRepository.findById(99L)).thenReturn(Mono.empty());
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(itemService.getItem(99L))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().contains("99"))
                .verify();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Item item(long id, String title, long price) {
        Item i = new Item();
        i.setId(id);
        i.setTitle(title);
        i.setPrice(price);
        i.setDescription("desc");
        i.setImgPath("img.svg");
        return i;
    }

    private List<Item> threeItems() {
        return List.of(
                item(1L, "Alpha", 300L),
                item(2L, "Beta", 100L),
                item(3L, "Gamma", 200L));
    }
}
