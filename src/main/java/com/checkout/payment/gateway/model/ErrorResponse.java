package com.checkout.payment.gateway.model;

import java.time.Instant;

public class ErrorResponse {
  private final String errorMessage;
  private final Instant timestamp;

  public ErrorResponse(String errorMessage) {
    this.errorMessage = errorMessage;
    this.timestamp = Instant.now();
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "ErrorResponse{" +
        "errorMessage='" + errorMessage + '\'' +
        ", timestamp=" + timestamp +
        '}';
  }
}
