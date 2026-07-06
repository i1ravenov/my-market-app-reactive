package org.mymarketapp.reactive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mymarketapp.reactive.dto.ItemDto;
import org.mymarketapp.reactive.dto.PageDto;
import org.mymarketapp.reactive.dto.SortType;
import org.mymarketapp.reactive.model.Item;
import org.mymarketapp.reactive.repository.CartItemRepository;
import org.mymarketapp.reactive.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Exercises the real {@code @Cacheable} AOP behind {@link ItemService} against an in-memory
 * {@link ConcurrentMapCacheManager} (no Redis needed) to verify that cache keys and cache
 * regions are correct - i.e. that different pages/sorts don't collide on the same entry, and
 * that {@code getItemsPage}/{@code buildPageDto} (different cache names, same key shape) don't
 * clash with each other.
 */
@SpringBootTest(classes = {ItemService.class, ItemServiceCacheKeyTest.CacheTestConfig.class})
class ItemServiceCacheKeyTest {

    @Configuration
    @EnableCaching
    static class CacheTestConfig {
        @Bean
        ConcurrentMapCacheManager cacheManager() {
            return new ConcurrentMapCacheManager("items", "page", "item");
        }
    }

    @MockitoBean
    ItemRepository itemRepository;
    @MockitoBean
    CartItemRepository cartItemRepository;

    @Autowired
    ItemService itemService;

    @Autowired
    CacheManager cacheManager;

    // caches persist across test methods (the Spring context/CacheManager bean is reused),
    // so entries from one test would otherwise be seen as cache hits by the next
    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void getItemsPage_sameArgsTwice_secondCallIsServedFromCache() {
        when(itemRepository.findPage(5, 0L)).thenReturn(Flux.just(item(1L, "Мяч", 500L)));
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());

        itemService.getItemsPage("", SortType.NO, 1, 5).block();
        itemService.getItemsPage("", SortType.NO, 1, 5).block();

        Mockito.verify(itemRepository, Mockito.times(1)).findPage(5, 0L);
    }

    @Test
    void getItemsPage_differentPageNumber_doesNotCollideWithCachedEntry() {
        when(itemRepository.findPage(5, 0L)).thenReturn(Flux.just(item(1L, "Мяч", 500L)));
        when(itemRepository.findPage(5, 5L)).thenReturn(Flux.just(item(2L, "Ракетка", 1500L)));
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());

        List<ItemDto> page1 = itemService.getItemsPage("", SortType.NO, 1, 5).block();
        List<ItemDto> page2 = itemService.getItemsPage("", SortType.NO, 2, 5).block();

        assertThat(page1.getFirst().title()).isEqualTo("Мяч");
        assertThat(page2.getFirst().title()).isEqualTo("Ракетка");
        Mockito.verify(itemRepository).findPage(5, 0L);
        Mockito.verify(itemRepository).findPage(5, 5L);
    }

    @Test
    void getItemsPage_differentSort_doesNotCollideWithCachedEntry() {
        when(itemRepository.findPage(5, 0L)).thenReturn(Flux.just(item(1L, "Мяч", 500L)));
        when(itemRepository.findPageOrderByTitle(5, 0L)).thenReturn(Flux.just(item(1L, "Мяч", 500L)));
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());

        itemService.getItemsPage("", SortType.NO, 1, 5).block();
        itemService.getItemsPage("", SortType.ALPHA, 1, 5).block();

        Mockito.verify(itemRepository).findPage(5, 0L);
        Mockito.verify(itemRepository).findPageOrderByTitle(5, 0L);
    }

    @Test
    void getItemsPage_differentSearch_doesNotCollideWithCachedEntry() {
        when(itemRepository.findPage(5, 0L)).thenReturn(Flux.just(item(1L, "Мяч", 500L)));
        when(itemRepository.findPageBySearch("ракетка", 5, 0L)).thenReturn(Flux.just(item(2L, "Ракетка", 1500L)));
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());

        itemService.getItemsPage("", SortType.NO, 1, 5).block();
        itemService.getItemsPage("ракетка", SortType.NO, 1, 5).block();

        Mockito.verify(itemRepository).findPage(5, 0L);
        Mockito.verify(itemRepository).findPageBySearch("ракетка", 5, 0L);
    }

    @Test
    void getItem_sameId_secondCallIsServedFromCache() {
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item(1L, "Мяч", 500L)));
        when(cartItemRepository.findById(1L)).thenReturn(Mono.empty());

        itemService.getItem(1L).block();
        itemService.getItem(1L).block();

        Mockito.verify(itemRepository, Mockito.times(1)).findById(1L);
    }

    @Test
    void getItem_differentIds_doNotCollideWithCachedEntry() {
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item(1L, "Мяч", 500L)));
        when(itemRepository.findById(2L)).thenReturn(Mono.just(item(2L, "Ракетка", 1500L)));
        when(cartItemRepository.findById(1L)).thenReturn(Mono.empty());
        when(cartItemRepository.findById(2L)).thenReturn(Mono.empty());

        ItemDto first = itemService.getItem(1L).block();
        ItemDto second = itemService.getItem(2L).block();

        assertThat(first.title()).isEqualTo("Мяч");
        assertThat(second.title()).isEqualTo("Ракетка");
    }

    @Test
    void getItemsPageAndBuildPageDto_sameArgs_useSeparateCacheRegionsWithoutCollision() {
        when(itemRepository.findPage(5, 0L)).thenReturn(Flux.just(item(1L, "Мяч", 500L)));
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());
        when(itemRepository.count()).thenReturn(Mono.just(1L));

        List<ItemDto> items = itemService.getItemsPage("", SortType.NO, 1, 5).block();
        PageDto page = itemService.buildPageDto("", SortType.NO, 1, 5).block();

        assertThat(items).hasSize(1);
        assertThat(page.pageNumber()).isEqualTo(1);
        assertThat(page.pageSize()).isEqualTo(5);
    }

    private Item item(long id, String title, long price) {
        Item i = new Item();
        i.setId(id);
        i.setTitle(title);
        i.setPrice(price);
        i.setDescription("desc");
        i.setImgPath("img.svg");
        return i;
    }
}
