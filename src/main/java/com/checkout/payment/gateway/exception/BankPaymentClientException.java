package com.checkout.payment.gateway.exception;

public class BankPaymentClientException extends RuntimeException {

  public BankPaymentClientException(String message) {
    super(message);
  }
}
