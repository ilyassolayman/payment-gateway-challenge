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
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PaymentResponse;
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

  private PaymentRequest validRequest() {
    PaymentRequest request = new PaymentRequest();
    request.setCardNumber("2222405343248877");
    request.setExpiryMonth(4);
    request.setExpiryYear(2027);
    request.setCurrency("GBP");
    request.setAmount(100);
    request.setCvv("123");
    return request;
  }

  // ── processPayment — card number normalisation ───────────────────────────────

  @Test
  void whenCardNumberHasSpaces_spacesAreStrippedBeforeProcessing() {
    when(bankPaymentService.processPayment(any(BankPaymentRequest.class)))
        .thenReturn(new BankPaymentResponse(true, "AUTH-123"));

    PaymentRequest request = validRequest();
    request.setCardNumber("2222 4053 4324 8877");

    PaymentResponse response = paymentService.processPayment(request);

    assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
    assertEquals("8877", response.getCardNumberLastFour());
  }

  // ── processPayment — validation rejected ─────────────────────────────────────

  @Test
  void whenValidationFails_returnsRejectedStatus() {
    PaymentRequest request = validRequest();
    request.setCardNumber(null);

    PaymentResponse response = paymentService.processPayment(request);

    assertEquals(PaymentStatus.REJECTED, response.getStatus());
  }

  @Test
  void whenValidationFails_rejectionReasonsArePopulated() {
    PaymentRequest request = validRequest();
    request.setCardNumber(null);

    PaymentResponse response = paymentService.processPayment(request);

    assertFalse(response.getRejectionReasons().isEmpty());
  }

  @Test
  void whenValidationFails_multipleErrorsAreAllReturned() {
    PaymentRequest request = validRequest();
    request.setCardNumber(null);
    request.setAmount(0);

    PaymentResponse response = paymentService.processPayment(request);

    assertEquals(2, response.getRejectionReasons().size());
  }

  @Test
  void whenValidationFails_bankServiceIsNeverCalled() {
    PaymentRequest request = validRequest();
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

    PaymentResponse response = paymentService.processPayment(validRequest());

    assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
  }

  @Test
  void whenBankAuthorizes_responseIsStoredInRepository() {
    when(bankPaymentService.processPayment(any(BankPaymentRequest.class)))
        .thenReturn(new BankPaymentResponse(true, "AUTH-123"));

    paymentService.processPayment(validRequest());

    verify(paymentsRepository).add(any(Payment.class));
  }

  @Test
  void whenBankAuthorizes_responseHasNonNullId() {
    when(bankPaymentService.processPayment(any(BankPaymentRequest.class)))
        .thenReturn(new BankPaymentResponse(true, "AUTH-123"));

    PaymentResponse response = paymentService.processPayment(validRequest());

    assertNotNull(response.getId());
  }

  // ── processPayment — bank declined ───────────────────────────────────────────

  @Test
  void whenBankDeclines_returnsDeclinedStatus() {
    when(bankPaymentService.processPayment(any(BankPaymentRequest.class)))
        .thenReturn(new BankPaymentResponse(false, null));

    PaymentResponse response = paymentService.processPayment(validRequest());

    assertEquals(PaymentStatus.DECLINED, response.getStatus());
  }

  @Test
  void whenBankDeclines_responseIsStoredInRepository() {
    when(bankPaymentService.processPayment(any(BankPaymentRequest.class)))
        .thenReturn(new BankPaymentResponse(false, null));

    paymentService.processPayment(validRequest());

    verify(paymentsRepository).add(any(Payment.class));
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
    Payment stored = new Payment(id, PaymentStatus.AUTHORIZED, "8877", 4, 2027, "GBP", 100, "AUTH-123");
    when(paymentsRepository.get(id)).thenReturn(Optional.of(stored));

    PaymentResponse response = paymentService.getPaymentById(id);

    assertEquals(id, response.getId());
    assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
  }

  @Test
  void whenPaymentNotFound_throwsPaymentNotFoundException() {
    UUID id = UUID.randomUUID();
    when(paymentsRepository.get(id)).thenReturn(Optional.empty());

    assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentById(id));
  }
}
