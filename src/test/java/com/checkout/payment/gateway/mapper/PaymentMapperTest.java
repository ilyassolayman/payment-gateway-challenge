package com.checkout.payment.gateway.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import org.junit.jupiter.api.Test;

class PaymentMapperTest {

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

  // ── toBankPaymentRequest ──────────────────────────────────────────────────────

  @Test
  void toBankPaymentRequest_mapsCardNumberCurrencyAmountAndCvv() {
    PostPaymentRequest request = validRequest();

    BankPaymentRequest result = PaymentMapper.toBankPaymentRequest(request);

    assertEquals(request.getCardNumber(), result.getCardNumber());
    assertEquals(request.getCurrency(), result.getCurrency());
    assertEquals(request.getAmount(), result.getAmount());
    assertEquals(request.getCvv(), result.getCvv());
  }

  @Test
  void toBankPaymentRequest_formatsSingleDigitMonthWithLeadingZero() {
    PostPaymentRequest request = validRequest();
    request.setExpiryMonth(4);
    request.setExpiryYear(2027);

    BankPaymentRequest result = PaymentMapper.toBankPaymentRequest(request);

    assertEquals("04/2027", result.getExpiryDate());
  }

  @Test
  void toBankPaymentRequest_formatsDoubleDigitMonthWithoutPadding() {
    PostPaymentRequest request = validRequest();
    request.setExpiryMonth(12);
    request.setExpiryYear(2028);

    BankPaymentRequest result = PaymentMapper.toBankPaymentRequest(request);

    assertEquals("12/2028", result.getExpiryDate());
  }

  // ── toPostPaymentResponse ─────────────────────────────────────────────────────

  @Test
  void toPostPaymentResponse_whenAuthorized_statusIsAuthorized() {
    BankPaymentResponse bankResponse = new BankPaymentResponse(true, "AUTH-001");

    PostPaymentResponse result = PaymentMapper.toPostPaymentResponse(validRequest(), bankResponse);

    assertEquals(PaymentStatus.AUTHORIZED, result.getStatus());
  }

  @Test
  void toPostPaymentResponse_whenNotAuthorized_statusIsDeclined() {
    BankPaymentResponse bankResponse = new BankPaymentResponse(false, null);

    PostPaymentResponse result = PaymentMapper.toPostPaymentResponse(validRequest(), bankResponse);

    assertEquals(PaymentStatus.DECLINED, result.getStatus());
  }

  @Test
  void toPostPaymentResponse_extractsLastFourDigitsOfCard() {
    PostPaymentRequest request = validRequest();
    request.setCardNumber("2222405343248877");
    BankPaymentResponse bankResponse = new BankPaymentResponse(true, "AUTH-001");

    PostPaymentResponse result = PaymentMapper.toPostPaymentResponse(request, bankResponse);

    assertEquals("8877", result.getCardNumberLastFour());
  }

  @Test
  void toPostPaymentResponse_mapsExpiryMonthYearCurrencyAndAmount() {
    PostPaymentRequest request = validRequest();
    BankPaymentResponse bankResponse = new BankPaymentResponse(true, "AUTH-001");

    PostPaymentResponse result = PaymentMapper.toPostPaymentResponse(request, bankResponse);

    assertEquals(request.getExpiryMonth(), result.getExpiryMonth());
    assertEquals(request.getExpiryYear(), result.getExpiryYear());
    assertEquals(request.getCurrency(), result.getCurrency());
    assertEquals(request.getAmount(), result.getAmount());
  }

  @Test
  void toPostPaymentResponse_generatesNonNullId() {
    BankPaymentResponse bankResponse = new BankPaymentResponse(true, "AUTH-001");

    PostPaymentResponse result = PaymentMapper.toPostPaymentResponse(validRequest(), bankResponse);

    assertNotNull(result.getId());
  }

  @Test
  void toPostPaymentResponse_generatesDifferentIdOnEachCall() {
    BankPaymentResponse bankResponse = new BankPaymentResponse(true, "AUTH-001");

    PostPaymentResponse result1 = PaymentMapper.toPostPaymentResponse(validRequest(), bankResponse);
    PostPaymentResponse result2 = PaymentMapper.toPostPaymentResponse(validRequest(), bankResponse);

    assertNotEquals(result1.getId(), result2.getId());
  }
}
