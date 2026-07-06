package org.mymarketapp.reactive.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mymarketapp.reactive.dto.ActionType;
import org.mymarketapp.reactive.dto.CheckoutStatus;
import org.mymarketapp.reactive.model.CartItem;
import org.mymarketapp.reactive.model.Item;
import org.mymarketapp.reactive.payment.BalanceResult;
import org.mymarketapp.reactive.payment.PaymentClient;
import org.mymarketapp.reactive.repository.CartItemRepository;
import org.mymarketapp.reactive.repository.ItemRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    CartItemRepository cartItemRepository;
    @Mock
    ItemRepository itemRepository;
    @Mock
    PaymentClient paymentClient;
    @InjectMocks
    CartService cartService;

    // ── changeCount ───────────────────────────────────────────────────────────

    @Test
    void changeCount_PLUS_newItem_insertsCartItemWithCountOne() {
        when(cartItemRepository.findById(1L)).thenReturn(Mono.empty());
        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(cartService.changeCount(1L, ActionType.PLUS))
                .verifyComplete();

        verify(cartItemRepository).save(argThat(ci -> ci.getItemId().equals(1L) && ci.getCount() == 1));
    }

    @Test
    void changeCount_PLUS_existingItem_incrementsCount() {
        CartItem existing = cartItemWithCount(2L, 3);
        when(cartItemRepository.findById(2L)).thenReturn(Mono.just(existing));
        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(cartService.changeCount(2L, ActionType.PLUS))
                .verifyComplete();

        verify(cartItemRepository).save(argThat(ci -> ci.getCount() == 4));
    }

    @Test
    void changeCount_MINUS_countAboveOne_decrementsCount() {
        CartItem existing = cartItemWithCount(3L, 5);
        when(cartItemRepository.findById(3L)).thenReturn(Mono.just(existing));
        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(cartService.changeCount(3L, ActionType.MINUS))
                .verifyComplete();

        verify(cartItemRepository).save(argThat(ci -> ci.getCount() == 4));
    }

    @Test
    void changeCount_MINUS_countEqualsOne_deletesItem() {
        CartItem existing = cartItemWithCount(4L, 1);
        when(cartItemRepository.findById(4L)).thenReturn(Mono.just(existing));
        when(cartItemRepository.deleteById(4L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeCount(4L, ActionType.MINUS))
                .verifyComplete();

        verify(cartItemRepository).deleteById(4L);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void changeCount_MINUS_itemNotInCart_doesNothing() {
        when(cartItemRepository.findById(5L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeCount(5L, ActionType.MINUS))
                .verifyComplete();

        verify(cartItemRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteById(anyLong());
    }

    @Test
    void changeCount_DELETE_callsDeleteById() {
        when(cartItemRepository.deleteById(6L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeCount(6L, ActionType.DELETE))
                .verifyComplete();

        verify(cartItemRepository).deleteById(6L);
        verify(cartItemRepository, never()).findById(anyLong());
    }

    // ── getCartItems ──────────────────────────────────────────────────────────

    @Test
    void getCartItems_returnsItemDtosWithCorrectData() {
        CartItem ci1 = cartItemWithCount(1L, 2);
        CartItem ci2 = cartItemWithCount(2L, 1);
        when(cartItemRepository.findAll()).thenReturn(Flux.just(ci1, ci2));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(itemWith(1L, "Мяч", 500L)));
        when(itemRepository.findById(2L)).thenReturn(Mono.just(itemWith(2L, "Ракетка", 1500L)));

        StepVerifier.create(cartService.getCartItems())
                .assertNext(dto -> {
                    assertThat(dto.title()).isEqualTo("Мяч");
                    assertThat(dto.count()).isEqualTo(2);
                    assertThat(dto.price()).isEqualTo(500L);
                })
                .assertNext(dto -> {
                    assertThat(dto.title()).isEqualTo("Ракетка");
                    assertThat(dto.count()).isEqualTo(1);
                })
                .verifyComplete();
    }

    @Test
    void getCartItems_filtersOutZeroCountItems() {
        when(cartItemRepository.findAll()).thenReturn(Flux.just(cartItemWithCount(1L, 0)));

        StepVerifier.create(cartService.getCartItems())
                .verifyComplete();

        verify(itemRepository, never()).findById(anyLong());
    }

    // ── getTotalSum ───────────────────────────────────────────────────────────

    @Test
    void getTotalSum_calculatesCorrectSum() {
        when(cartItemRepository.findAll()).thenReturn(
                Flux.just(cartItemWithCount(1L, 2), cartItemWithCount(2L, 3)));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(itemWith(1L, "A", 100L)));
        when(itemRepository.findById(2L)).thenReturn(Mono.just(itemWith(2L, "B", 200L)));

        // 2*100 + 3*200 = 200 + 600 = 800
        StepVerifier.create(cartService.getTotalSum())
                .expectNext(800L)
                .verifyComplete();
    }

    @Test
    void getTotalSum_emptyCart_returnsZero() {
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(cartService.getTotalSum())
                .expectNext(0L)
                .verifyComplete();
    }

    // ── clearCart ─────────────────────────────────────────────────────────────

    @Test
    void clearCart_callsDeleteAll() {
        when(cartItemRepository.deleteAll()).thenReturn(Mono.empty());

        StepVerifier.create(cartService.clearCart())
                .verifyComplete();

        verify(cartItemRepository).deleteAll();
    }

    // ── canCheckout ───────────────────────────────────────────────────────────

    @Test
    void canCheckout_sufficientBalance_returnsOk() {
        when(cartItemRepository.findAll()).thenReturn(Flux.just(cartItemWithCount(1L, 2)));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(itemWith(1L, "A", 100L)));
        when(paymentClient.getBalance()).thenReturn(Mono.just(new BalanceResult(true, 1000L)));

        StepVerifier.create(cartService.canCheckout())
                .expectNext(CheckoutStatus.OK)
                .verifyComplete();
    }

    @Test
    void canCheckout_insufficientBalance_returnsInsufficientBalance() {
        when(cartItemRepository.findAll()).thenReturn(Flux.just(cartItemWithCount(1L, 2)));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(itemWith(1L, "A", 100L)));
        when(paymentClient.getBalance()).thenReturn(Mono.just(new BalanceResult(true, 100L)));

        StepVerifier.create(cartService.canCheckout())
                .expectNext(CheckoutStatus.INSUFFICIENT_BALANCE)
                .verifyComplete();
    }

    @Test
    void canCheckout_paymentServiceUnavailable_returnsUnavailable() {
        when(cartItemRepository.findAll()).thenReturn(Flux.just(cartItemWithCount(1L, 2)));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(itemWith(1L, "A", 100L)));
        when(paymentClient.getBalance()).thenReturn(Mono.just(BalanceResult.unavailable()));

        StepVerifier.create(cartService.canCheckout())
                .expectNext(CheckoutStatus.PAYMENT_SERVICE_UNAVAILABLE)
                .verifyComplete();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CartItem cartItemWithCount(long itemId, int count) {
        CartItem ci = new CartItem();
        ci.setItemId(itemId);
        ci.setCount(count);
        return ci;
    }

    private Item itemWith(long id, String title, long price) {
        Item item = new Item();
        item.setId(id);
        item.setTitle(title);
        item.setPrice(price);
        item.setDescription("desc");
        item.setImgPath("img.svg");
        return item;
    }
}
