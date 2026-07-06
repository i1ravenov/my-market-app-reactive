package org.mymarketapp.reactive.config;

import org.mymarketapp.reactive.paymentclient.ApiClient;
import org.mymarketapp.reactive.paymentclient.api.PaymentApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentClientConfig {

    @Bean
    public ApiClient paymentApiClient(@Value("${payment.service.url}") String paymentServiceUrl) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(paymentServiceUrl);
        return apiClient;
    }

    @Bean
    public PaymentApi paymentApi(ApiClient paymentApiClient) {
        return new PaymentApi(paymentApiClient);
    }
}
