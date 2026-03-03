package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.exception.BankPaymentClientException;
import com.checkout.payment.gateway.exception.BankPaymentRequestException;
import com.checkout.payment.gateway.exception.BankPaymentResponseException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class BankPaymentServiceTest {

  @Mock
  private RestTemplate restTemplate;

  private BankPaymentService bankPaymentService;

  private static final String BANK_URL = "http://localhost:8080/payments";

  @BeforeEach
  void setUp() {
    bankPaymentService = new BankPaymentService(restTemplate, BANK_URL);
  }

  private BankPaymentRequest aRequest() {
    return new BankPaymentRequest("2222405343248877", "04/2027", "GBP", 100, "123");
  }

  // ── Successful responses ──────────────────────────────────────────────────────

  @Test
  void whenBankReturnsAuthorized_returnsAuthorizedResponse() {
    BankPaymentResponse bankResponse = new BankPaymentResponse(true, "AUTH-001");
    when(restTemplate.postForEntity(eq(BANK_URL), any(), eq(BankPaymentResponse.class)))
        .thenReturn(ResponseEntity.ok(bankResponse));

    BankPaymentResponse result = bankPaymentService.processPayment(aRequest());

    assertTrue(result.isAuthorized());
    assertEquals("AUTH-001", result.getAuthorizationCode());
  }

  @Test
  void whenBankReturnsDeclined_returnsDeclinedResponse() {
    BankPaymentResponse bankResponse = new BankPaymentResponse(false, null);
    when(restTemplate.postForEntity(eq(BANK_URL), any(), eq(BankPaymentResponse.class)))
        .thenReturn(ResponseEntity.ok(bankResponse));

    BankPaymentResponse result = bankPaymentService.processPayment(aRequest());

    assertFalse(result.isAuthorized());
  }

  // ── Exception handling ────────────────────────────────────────────────────────

  @Test
  void whenBankReturns4xx_throwsBankPaymentClientException() {
    when(restTemplate.postForEntity(eq(BANK_URL), any(), eq(BankPaymentResponse.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

    assertThrows(BankPaymentClientException.class,
        () -> bankPaymentService.processPayment(aRequest()));
  }

  @Test
  void whenBankReturns5xx_throwsBankPaymentRequestException() {
    when(restTemplate.postForEntity(eq(BANK_URL), any(), eq(BankPaymentResponse.class)))
        .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

    assertThrows(BankPaymentRequestException.class,
        () -> bankPaymentService.processPayment(aRequest()));
  }

  @Test
  void whenBankIsUnreachable_throwsBankPaymentRequestException() {
    when(restTemplate.postForEntity(eq(BANK_URL), any(), eq(BankPaymentResponse.class)))
        .thenThrow(new ResourceAccessException("Connection refused"));

    assertThrows(BankPaymentRequestException.class,
        () -> bankPaymentService.processPayment(aRequest()));
  }

  @Test
  void whenBankResponseBodyIsNull_throwsBankPaymentResponseException() {
    when(restTemplate.postForEntity(eq(BANK_URL), any(), eq(BankPaymentResponse.class)))
        .thenReturn(ResponseEntity.ok(null));

    assertThrows(BankPaymentResponseException.class,
        () -> bankPaymentService.processPayment(aRequest()));
  }
}
