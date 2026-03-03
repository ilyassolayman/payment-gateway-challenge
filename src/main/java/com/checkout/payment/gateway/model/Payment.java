package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import java.util.UUID;

public class Payment {

  private final UUID id;
  private final PaymentStatus status;
  private final String cardNumberLastFour;
  private final Integer expiryMonth;
  private final Integer expiryYear;
  private final String currency;
  private final Integer amount;
  private final String authorizationCode;

  public Payment(UUID id, PaymentStatus status, String cardNumberLastFour,
      Integer expiryMonth, Integer expiryYear, String currency,
      Integer amount, String authorizationCode) {
    this.id = id;
    this.status = status;
    this.cardNumberLastFour = cardNumberLastFour;
    this.expiryMonth = expiryMonth;
    this.expiryYear = expiryYear;
    this.currency = currency;
    this.amount = amount;
    this.authorizationCode = authorizationCode;
  }

  public UUID getId() { return id; }
  public PaymentStatus getStatus() { return status; }
  public String getCardNumberLastFour() { return cardNumberLastFour; }
  public Integer getExpiryMonth() { return expiryMonth; }
  public Integer getExpiryYear() { return expiryYear; }
  public String getCurrency() { return currency; }
  public Integer getAmount() { return amount; }
  public String getAuthorizationCode() { return authorizationCode; }
}
