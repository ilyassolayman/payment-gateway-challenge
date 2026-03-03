package com.checkout.payment.gateway.mapper;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import java.util.UUID;

public class PaymentMapper {

  private PaymentMapper() {}

  public static BankPaymentRequest toBankPaymentRequest(PostPaymentRequest request) {
    return new BankPaymentRequest(
        request.getCardNumber(),
        String.format("%02d/%d", request.getExpiryMonth(), request.getExpiryYear()),
        request.getCurrency(),
        request.getAmount(),
        request.getCvv()
    );
  }

  public static PostPaymentResponse toPostPaymentResponse(PostPaymentRequest request, BankPaymentResponse bankResponse) {
    String lastFour = request.getCardNumber().substring(request.getCardNumber().length() - 4);
    return PostPaymentResponse.builder()
        .id(UUID.randomUUID())
        .status(bankResponse.isAuthorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED)
        .cardNumberLastFour(lastFour)
        .expiryMonth(request.getExpiryMonth())
        .expiryYear(request.getExpiryYear())
        .currency(request.getCurrency())
        .amount(request.getAmount())
        .build();
  }
}
