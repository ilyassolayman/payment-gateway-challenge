package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PaymentResponse;
import java.util.UUID;

public interface IPaymentService {

  PaymentResponse getPaymentById(UUID id);

  PaymentResponse processPayment(PaymentRequest request);
}
