package com.checkout.payment.gateway.exception;

public class BankPaymentRequestException extends RuntimeException {

  public BankPaymentRequestException(String message) {
    super(message);
  }
}
