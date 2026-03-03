package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import java.util.UUID;

public interface IPaymentService {

  PostPaymentResponse getPaymentById(UUID id);

  PostPaymentResponse processPayment(PostPaymentRequest request);
}
