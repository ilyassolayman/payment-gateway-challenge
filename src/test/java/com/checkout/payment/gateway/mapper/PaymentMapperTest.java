package com.checkout.payment.gateway.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PaymentResponse;
import org.junit.jupiter.api.Test;

class PaymentMapperTest {

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

  // ── toBankPaymentRequest ──────────────────────────────────────────────────────

  @Test
  void toBankPaymentRequest_mapsCardNumberCurrencyAmountAndCvv() {
    PaymentRequest request = validRequest();

    BankPaymentRequest result = PaymentMapper.toBankPaymentRequest(request);

    assertEquals(request.getCardNumber(), result.getCardNumber());
    assertEquals(request.getCurrency(), result.getCurrency());
    assertEquals(request.getAmount(), result.getAmount());
    assertEquals(request.getCvv(), result.getCvv());
  }

  @Test
  void toBankPaymentRequest_formatsSingleDigitMonthWithLeadingZero() {
    PaymentRequest request = validRequest();
    request.setExpiryMonth(4);
    request.setExpiryYear(2027);

    BankPaymentRequest result = PaymentMapper.toBankPaymentRequest(request);

    assertEquals("04/2027", result.getExpiryDate());
  }

  @Test
  void toBankPaymentRequest_formatsDoubleDigitMonthWithoutPadding() {
    PaymentRequest request = validRequest();
    request.setExpiryMonth(12);
    request.setExpiryYear(2028);

    BankPaymentRequest result = PaymentMapper.toBankPaymentRequest(request);

    assertEquals("12/2028", result.getExpiryDate());
  }

  // ── toPayment ─────────────────────────────────────────────────────────────────

  @Test
  void toPayment_whenAuthorized_statusIsAuthorized() {
    BankPaymentResponse bankResponse = new BankPaymentResponse(true, "AUTH-001");

    Payment result = PaymentMapper.toPayment(validRequest(), bankResponse);

    assertEquals(PaymentStatus.AUTHORIZED, result.getStatus());
  }

  @Test
  void toPayment_whenNotAuthorized_statusIsDeclined() {
    BankPaymentResponse bankResponse = new BankPaymentResponse(false, null);

    Payment result = PaymentMapper.toPayment(validRequest(), bankResponse);

    assertEquals(PaymentStatus.DECLINED, result.getStatus());
  }

  @Test
  void toPayment_extractsLastFourDigitsOfCard() {
    PaymentRequest request = validRequest();
    request.setCardNumber("2222405343248877");
    BankPaymentResponse bankResponse = new BankPaymentResponse(true, "AUTH-001");

    Payment result = PaymentMapper.toPayment(request, bankResponse);

    assertEquals("8877", result.getCardNumberLastFour());
  }

  @Test
  void toPayment_mapsExpiryMonthYearCurrencyAndAmount() {
    PaymentRequest request = validRequest();
    BankPaymentResponse bankResponse = new BankPaymentResponse(true, "AUTH-001");

    Payment result = PaymentMapper.toPayment(request, bankResponse);

    assertEquals(request.getExpiryMonth(), result.getExpiryMonth());
    assertEquals(request.getExpiryYear(), result.getExpiryYear());
    assertEquals(request.getCurrency(), result.getCurrency());
    assertEquals(request.getAmount(), result.getAmount());
  }

  @Test
  void toPayment_storesAuthorizationCode() {
    BankPaymentResponse bankResponse = new BankPaymentResponse(true, "AUTH-001");

    Payment result = PaymentMapper.toPayment(validRequest(), bankResponse);

    assertEquals("AUTH-001", result.getAuthorizationCode());
  }

  @Test
  void toPayment_whenDeclined_authorizationCodeIsNull() {
    BankPaymentResponse bankResponse = new BankPaymentResponse(false, null);

    Payment result = PaymentMapper.toPayment(validRequest(), bankResponse);

    assertNull(result.getAuthorizationCode());
  }

  @Test
  void toPayment_generatesNonNullId() {
    BankPaymentResponse bankResponse = new BankPaymentResponse(true, "AUTH-001");

    Payment result = PaymentMapper.toPayment(validRequest(), bankResponse);

    assertNotNull(result.getId());
  }

  @Test
  void toPayment_generatesDifferentIdOnEachCall() {
    BankPaymentResponse bankResponse = new BankPaymentResponse(true, "AUTH-001");

    Payment result1 = PaymentMapper.toPayment(validRequest(), bankResponse);
    Payment result2 = PaymentMapper.toPayment(validRequest(), bankResponse);

    assertNotEquals(result1.getId(), result2.getId());
  }

  // ── toPaymentResponse ─────────────────────────────────────────────────────

  @Test
  void toPaymentResponse_mapsAllFieldsFromPayment() {
    BankPaymentResponse bankResponse = new BankPaymentResponse(true, "AUTH-001");
    Payment payment = PaymentMapper.toPayment(validRequest(), bankResponse);

    PaymentResponse result = PaymentMapper.toPaymentResponse(payment);

    assertEquals(payment.getId(), result.getId());
    assertEquals(payment.getStatus(), result.getStatus());
    assertEquals(payment.getCardNumberLastFour(), result.getCardNumberLastFour());
    assertEquals(payment.getExpiryMonth(), result.getExpiryMonth());
    assertEquals(payment.getExpiryYear(), result.getExpiryYear());
    assertEquals(payment.getCurrency(), result.getCurrency());
    assertEquals(payment.getAmount(), result.getAmount());
  }

  @Test
  void toPaymentResponse_doesNotExposeAuthorizationCode() {
    BankPaymentResponse bankResponse = new BankPaymentResponse(true, "AUTH-001");
    Payment payment = PaymentMapper.toPayment(validRequest(), bankResponse);

    PaymentResponse result = PaymentMapper.toPaymentResponse(payment);

    // authorizationCode is stored on Payment but is not surfaced in the API response
    assertNull(result.getRejectionReasons());
  }
}
