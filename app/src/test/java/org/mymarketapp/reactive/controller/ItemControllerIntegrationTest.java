package org.mymarketapp.reactive.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mymarketapp.reactive.repository.CartItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
class ItemControllerIntegrationTest {

    @Autowired WebTestClient webTestClient;
    @Autowired CartItemRepository cartItemRepository;

    @BeforeEach
    void cleanCart() {
        cartItemRepository.deleteAll().block();
    }

    @Test
    void getRoot_redirectsToItems() {
        webTestClient.get().uri("/")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/items.*");
    }

    @Test
    void getItems_returnsHtmlWithItemGrid() {
        webTestClient.get().uri("/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("Футбольный мяч");
                    assertThat(body).contains("Теннисная ракетка");
                    assertThat(body).contains("Велосипед");
                });
    }

    @Test
    void getItems_withPageSize_returnsPaginatedResult() {
        webTestClient.get().uri("/items?pageSize=3&pageNumber=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("card-title"));
    }

    @Test
    void getItems_withSearch_returnsFilteredItems() {
        webTestClient.get().uri(uri -> uri.path("/items")
                        .queryParam("search", "мяч").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("мяч");
                    assertThat(body).doesNotContain("Велосипед");
                });
    }

    @Test
    void getItems_withSortAlpha_returnsHtml() {
        webTestClient.get().uri("/items?sort=ALPHA")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("sort"));
    }

    @Test
    void getItemDetail_existingItem_returnsHtmlWithItemInfo() {
        webTestClient.get().uri("/items/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("Футбольный мяч"));
    }

    @Test
    void getItemDetail_nonExistentItem_returnsError() {
        webTestClient.get().uri("/items/9999")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void postItems_addToCart_redirectsToItems() {
        webTestClient.post().uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=1&action=PLUS&search=&sort=NO&pageNumber=1&pageSize=5")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/items.*");
    }

    @Test
    void postItems_addToCart_cartCountIncreases() {
        webTestClient.post().uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=1&action=PLUS&search=&sort=NO&pageNumber=1&pageSize=5")
                .exchange()
                .expectStatus().is3xxRedirection();

        Long count = cartItemRepository.findById(1L).block().getCount().longValue();
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void postItemDetail_addToCart_returnsUpdatedItemPage() {
        webTestClient.post().uri("/items/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("action=PLUS")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("Футбольный мяч"));
    }
}
