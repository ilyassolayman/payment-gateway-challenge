package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {
  private UUID id;
  private PaymentStatus status;
  private String cardNumberLastFour;
  private Integer expiryMonth;
  private Integer expiryYear;
  private String currency;
  private Integer amount;
  private List<String> rejectionReasons;

  private PaymentResponse() {}

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id;
    private PaymentStatus status;
    private String cardNumberLastFour;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String currency;
    private Integer amount;
    private List<String> rejectionReasons;

    private Builder() {}

    public Builder id(UUID id) { this.id = id; return this; }
    public Builder status(PaymentStatus status) { this.status = status; return this; }
    public Builder cardNumberLastFour(String cardNumberLastFour) { this.cardNumberLastFour = cardNumberLastFour; return this; }
    public Builder expiryMonth(Integer expiryMonth) { this.expiryMonth = expiryMonth; return this; }
    public Builder expiryYear(Integer expiryYear) { this.expiryYear = expiryYear; return this; }
    public Builder currency(String currency) { this.currency = currency; return this; }
    public Builder amount(Integer amount) { this.amount = amount; return this; }
    public Builder rejectionReasons(List<String> rejectionReasons) { this.rejectionReasons = rejectionReasons; return this; }

    public PaymentResponse build() {
      PaymentResponse response = new PaymentResponse();
      response.id = this.id;
      response.status = this.status;
      response.cardNumberLastFour = this.cardNumberLastFour;
      response.expiryMonth = this.expiryMonth;
      response.expiryYear = this.expiryYear;
      response.currency = this.currency;
      response.amount = this.amount;
      response.rejectionReasons = this.rejectionReasons;
      return response;
    }
  }

  public UUID getId() {
    return id;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public String getCardNumberLastFour() {
    return cardNumberLastFour;
  }

  public Integer getExpiryMonth() {
    return expiryMonth;
  }

  public Integer getExpiryYear() {
    return expiryYear;
  }

  public String getCurrency() {
    return currency;
  }

  public Integer getAmount() {
    return amount;
  }

  public List<String> getRejectionReasons() {
    return rejectionReasons;
  }

  @Override
  public String toString() {
    return "PaymentResponse{" +
        "id=" + id +
        ", status=" + status +
        ", cardNumberLastFour=" + cardNumberLastFour +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        ", rejectionReasons=" + rejectionReasons +
        '}';
  }
}
