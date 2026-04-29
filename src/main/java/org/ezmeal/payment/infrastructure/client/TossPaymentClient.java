package org.ezmeal.payment.infrastructure.client;

import org.ezmeal.payment.application.dto.request.TossConfirmRequest;
import org.ezmeal.payment.application.dto.response.TossConfirmResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "tossPaymentClient", url = "${payment.toss.url}")
public interface TossPaymentClient {

    @PostMapping("/confirm")
    TossConfirmResponse confirmPayment(
            @RequestHeader("Authorization") String basicToken, // "Basic " + Base64(SecretKey:)
            @RequestBody TossConfirmRequest request
    );
}
