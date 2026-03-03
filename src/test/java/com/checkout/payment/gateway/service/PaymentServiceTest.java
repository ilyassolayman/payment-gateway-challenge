package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.BankPaymentClientException;
import com.checkout.payment.gateway.exception.BankPaymentRequestException;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.IPaymentsRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock
  private IPaymentsRepository paymentsRepository;

  @Mock
  private IBankPaymentService bankPaymentService;

  @InjectMocks
  private PaymentService paymentService;

  private PostPaymentRequest validRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("2222405343248877");
    request.setExpiryMonth(4);
    request.setExpiryYear(2027);
    request.setCurrency("GBP");
    request.setAmount(100);
    request.setCvv("123");
    return request;
  }

  // ── processPayment — validation rejected ─────────────────────────────────────

  @Test
  void whenValidationFails_returnsRejectedStatus() {
    PostPaymentRequest request = validRequest();
    request.setCardNumber(null);

    PostPaymentResponse response = paymentService.processPayment(request);

    assertEquals(PaymentStatus.REJECTED, response.getStatus());
  }

  @Test
  void whenValidationFails_rejectionReasonsArePopulated() {
    PostPaymentRequest request = validRequest();
    request.setCardNumber(null);

    PostPaymentResponse response = paymentService.processPayment(request);

    assertFalse(response.getRejectionReasons().isEmpty());
  }

  @Test
  void whenValidationFails_multipleErrorsAreAllReturned() {
    PostPaymentRequest request = validRequest();
    request.setCardNumber(null);
    request.setAmount(0);

    PostPaymentResponse response = paymentService.processPayment(request);

    assertEquals(2, response.getRejectionReasons().size());
  }

  @Test
  void whenValidationFails_bankServiceIsNeverCalled() {
    PostPaymentRequest request = validRequest();
    request.setCardNumber(null);

    paymentService.processPayment(request);

    verifyNoInteractions(bankPaymentService);
    verifyNoInteractions(paymentsRepository);
  }

  // ── processPayment — bank authorized ─────────────────────────────────────────

  @Test
  void whenBankAuthorizes_returnsAuthorizedStatus() {
    when(bankPaymentService.processPayment(any(BankPaymentRequest.class)))
        .thenReturn(new BankPaymentResponse(true, "AUTH-123"));

    PostPaymentResponse response = paymentService.processPayment(validRequest());

    assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
  }

  @Test
  void whenBankAuthorizes_responseIsStoredInRepository() {
    when(bankPaymentService.processPayment(any(BankPaymentRequest.class)))
        .thenReturn(new BankPaymentResponse(true, "AUTH-123"));

    PostPaymentResponse response = paymentService.processPayment(validRequest());

    verify(paymentsRepository).add(response);
  }

  @Test
  void whenBankAuthorizes_responseHasNonNullId() {
    when(bankPaymentService.processPayment(any(BankPaymentRequest.class)))
        .thenReturn(new BankPaymentResponse(true, "AUTH-123"));

    PostPaymentResponse response = paymentService.processPayment(validRequest());

    assertNotNull(response.getId());
  }

  // ── processPayment — bank declined ───────────────────────────────────────────

  @Test
  void whenBankDeclines_returnsDeclinedStatus() {
    when(bankPaymentService.processPayment(any(BankPaymentRequest.class)))
        .thenReturn(new BankPaymentResponse(false, null));

    PostPaymentResponse response = paymentService.processPayment(validRequest());

    assertEquals(PaymentStatus.DECLINED, response.getStatus());
  }

  @Test
  void whenBankDeclines_responseIsStoredInRepository() {
    when(bankPaymentService.processPayment(any(BankPaymentRequest.class)))
        .thenReturn(new BankPaymentResponse(false, null));

    PostPaymentResponse response = paymentService.processPayment(validRequest());

    verify(paymentsRepository).add(response);
  }

  // ── processPayment — bank exceptions ─────────────────────────────────────────

  @Test
  void whenBankThrowsBankPaymentRequestException_exceptionPropagates() {
    when(bankPaymentService.processPayment(any(BankPaymentRequest.class)))
        .thenThrow(new BankPaymentRequestException("Bank unavailable"));

    assertThrows(BankPaymentRequestException.class,
        () -> paymentService.processPayment(validRequest()));
    verify(paymentsRepository, never()).add(any());
  }

  @Test
  void whenBankThrowsBankPaymentClientException_exceptionPropagates() {
    when(bankPaymentService.processPayment(any(BankPaymentRequest.class)))
        .thenThrow(new BankPaymentClientException("Bank rejected request"));

    assertThrows(BankPaymentClientException.class,
        () -> paymentService.processPayment(validRequest()));
    verify(paymentsRepository, never()).add(any());
  }

  // ── getPaymentById ────────────────────────────────────────────────────────────

  @Test
  void whenPaymentExists_returnsPayment() {
    UUID id = UUID.randomUUID();
    PostPaymentResponse stored = PostPaymentResponse.builder()
        .id(id)
        .status(PaymentStatus.AUTHORIZED)
        .build();
    when(paymentsRepository.get(id)).thenReturn(Optional.of(stored));

    PostPaymentResponse response = paymentService.getPaymentById(id);

    assertEquals(stored, response);
  }

  @Test
  void whenPaymentNotFound_throwsPaymentNotFoundException() {
    UUID id = UUID.randomUUID();
    when(paymentsRepository.get(id)).thenReturn(Optional.empty());

    assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentById(id));
  }
}
