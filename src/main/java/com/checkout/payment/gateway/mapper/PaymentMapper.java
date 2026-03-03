package com.checkout.payment.gateway.mapper;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PaymentResponse;
import java.util.UUID;

public class PaymentMapper {

  private PaymentMapper() {}

  public static BankPaymentRequest toBankPaymentRequest(PaymentRequest request) {
    return new BankPaymentRequest(
        request.getCardNumber(),
        String.format("%02d/%d", request.getExpiryMonth(), request.getExpiryYear()),
        request.getCurrency(),
        request.getAmount(),
        request.getCvv()
    );
  }

  public static Payment toPayment(PaymentRequest request, BankPaymentResponse bankResponse) {
    String lastFour = request.getCardNumber().substring(request.getCardNumber().length() - 4);
    return new Payment(
        UUID.randomUUID(),
        bankResponse.isAuthorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED,
        lastFour,
        request.getExpiryMonth(),
        request.getExpiryYear(),
        request.getCurrency(),
        request.getAmount(),
        bankResponse.getAuthorizationCode()
    );
  }

  public static PaymentResponse toPaymentResponse(Payment payment) {
    return PaymentResponse.builder()
        .id(payment.getId())
        .status(payment.getStatus())
        .cardNumberLastFour(payment.getCardNumberLastFour())
        .expiryMonth(payment.getExpiryMonth())
        .expiryYear(payment.getExpiryYear())
        .currency(payment.getCurrency())
        .amount(payment.getAmount())
        .build();
  }
}
