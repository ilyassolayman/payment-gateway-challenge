package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BankPaymentResponse {

  private boolean authorized;
  @JsonProperty("authorization_code")
  private String authorizationCode;

  public BankPaymentResponse() {
  }

  public BankPaymentResponse(boolean authorized, String authorizationCode) {
    this.authorized = authorized;
    this.authorizationCode = authorizationCode;
  }

  public boolean isAuthorized() {
    return authorized;
  }

  public void setAuthorized(boolean authorized) {
    this.authorized = authorized;
  }

  public String getAuthorizationCode() {
    return authorizationCode;
  }

  public void setAuthorizationCode(String authorizationCode) {
    this.authorizationCode = authorizationCode;
  }
}
