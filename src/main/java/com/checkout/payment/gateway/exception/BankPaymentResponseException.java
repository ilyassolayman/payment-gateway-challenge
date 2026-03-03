package com.checkout.payment.gateway.exception;

public class BankPaymentResponseException extends RuntimeException {

  public BankPaymentResponseException(String message) {
    super(message);
  }
}
