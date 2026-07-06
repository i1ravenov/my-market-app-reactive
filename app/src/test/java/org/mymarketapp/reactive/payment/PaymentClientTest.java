package org.mymarketapp.reactive.payment;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mymarketapp.reactive.paymentclient.ApiClient;
import org.mymarketapp.reactive.paymentclient.api.PaymentApi;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentClientTest {

    private WireMockServer wireMockServer;
    private PaymentClient paymentClient;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath("http://localhost:" + wireMockServer.port());
        paymentClient = new PaymentClient(new PaymentApi(apiClient));
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void getBalanceReturnsAvailableResultOnSuccess() {
        wireMockServer.stubFor(get(urlEqualTo("/balance"))
                .willReturn(okJson("{\"balance\": 5000}")));

        StepVerifier.create(paymentClient.getBalance())
                .assertNext(result -> {
                    assertThat(result.available()).isTrue();
                    assertThat(result.balance()).isEqualTo(5000L);
                })
                .verifyComplete();
    }

    @Test
    void getBalanceReturnsUnavailableWhenServiceRespondsWithError() {
        wireMockServer.stubFor(get(urlEqualTo("/balance")).willReturn(serverError()));

        StepVerifier.create(paymentClient.getBalance())
                .assertNext(result -> assertThat(result.available()).isFalse())
                .verifyComplete();
    }

    @Test
    void getBalanceReturnsUnavailableWhenServiceIsUnreachable() {
        wireMockServer.stop();

        StepVerifier.create(paymentClient.getBalance())
                .assertNext(result -> assertThat(result.available()).isFalse())
                .verifyComplete();
    }

    @Test
    void payReturnsSuccessfulResult() {
        wireMockServer.stubFor(post(urlEqualTo("/pay"))
                .willReturn(okJson("{\"success\": true, \"balance\": 3500}")));

        StepVerifier.create(paymentClient.pay(1500))
                .assertNext(result -> {
                    assertThat(result.available()).isTrue();
                    assertThat(result.success()).isTrue();
                    assertThat(result.balance()).isEqualTo(3500L);
                })
                .verifyComplete();
    }

    @Test
    void payReturnsUnsuccessfulResultWhenBalanceIsInsufficient() {
        wireMockServer.stubFor(post(urlEqualTo("/pay"))
                .willReturn(okJson("{\"success\": false, \"balance\": 1000}")));

        StepVerifier.create(paymentClient.pay(5000))
                .assertNext(result -> {
                    assertThat(result.available()).isTrue();
                    assertThat(result.success()).isFalse();
                    assertThat(result.balance()).isEqualTo(1000L);
                })
                .verifyComplete();
    }

    @Test
    void payReturnsUnavailableWhenServiceIsUnreachable() {
        wireMockServer.stop();

        StepVerifier.create(paymentClient.pay(100))
                .assertNext(result -> assertThat(result.available()).isFalse())
                .verifyComplete();
    }
}
