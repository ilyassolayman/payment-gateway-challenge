package com.checkout.payment.gateway.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.checkout.payment.gateway.enums.ValidationErrorCode;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.ValidationResult;
import org.junit.jupiter.api.Test;

class PaymentRequestValidatorTest {

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

  @Test
  void validRequestProducesNoErrors() {
    assertTrue(PaymentRequestValidator.validate(validRequest()).isValid());
  }

  // ── Card number ─────────────────────────────────────────────────────────────

  @Test
  void whenCardNumberIsNullThenCardNumberRequiredError() {
    PaymentRequest request = validRequest();
    request.setCardNumber(null);
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.CARD_NUMBER_REQUIRED, result.getErrors().get(0).code());
  }

  @Test
  void whenCardNumberIsBlankThenCardNumberRequiredError() {
    PaymentRequest request = validRequest();
    request.setCardNumber("   ");
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.CARD_NUMBER_REQUIRED, result.getErrors().get(0).code());
  }

  @Test
  void whenCardNumberTooShortThenInvalidLengthError() {
    PaymentRequest request = validRequest();
    request.setCardNumber("1234567890123"); // 13 digits
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.CARD_NUMBER_INVALID_LENGTH, result.getErrors().get(0).code());
  }

  @Test
  void whenCardNumberTooLongThenInvalidLengthError() {
    PaymentRequest request = validRequest();
    request.setCardNumber("12345678901234567890"); // 20 digits
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.CARD_NUMBER_INVALID_LENGTH, result.getErrors().get(0).code());
  }

  @Test
  void whenCardNumberContainsLettersThenNonNumericError() {
    PaymentRequest request = validRequest();
    request.setCardNumber("222240534324abcd");
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.CARD_NUMBER_NON_NUMERIC, result.getErrors().get(0).code());
  }

  @Test
  void whenCardNumberIs14DigitsThenValid() {
    PaymentRequest request = validRequest();
    request.setCardNumber("12345678901231");
    assertTrue(PaymentRequestValidator.validate(request).isValid());
  }

  @Test
  void whenCardNumberIs19DigitsThenValid() {
    PaymentRequest request = validRequest();
    request.setCardNumber("1234567890123456789");
    assertTrue(PaymentRequestValidator.validate(request).isValid());
  }

  // ── Expiry month ─────────────────────────────────────────────────────────────

  @Test
  void whenExpiryMonthIsNullThenExpiryMonthRequiredError() {
    PaymentRequest request = validRequest();
    request.setExpiryMonth(null);
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.EXPIRY_MONTH_REQUIRED, result.getErrors().get(0).code());
  }

  @Test
  void whenExpiryMonthIsZeroThenExpiryMonthInvalidError() {
    PaymentRequest request = validRequest();
    request.setExpiryMonth(0);
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.EXPIRY_MONTH_INVALID, result.getErrors().get(0).code());
  }

  @Test
  void whenExpiryMonthIs13ThenExpiryMonthInvalidError() {
    PaymentRequest request = validRequest();
    request.setExpiryMonth(13);
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.EXPIRY_MONTH_INVALID, result.getErrors().get(0).code());
  }

  @Test
  void whenExpiryMonthIs1ThenValid() {
    PaymentRequest request = validRequest();
    request.setExpiryMonth(1);
    request.setExpiryYear(2027);
    assertTrue(PaymentRequestValidator.validate(request).isValid());
  }

  @Test
  void whenExpiryMonthIs12ThenValid() {
    PaymentRequest request = validRequest();
    request.setExpiryMonth(12);
    request.setExpiryYear(2026);
    assertTrue(PaymentRequestValidator.validate(request).isValid());
  }

  // ── Expiry year ──────────────────────────────────────────────────────────────

  @Test
  void whenExpiryYearIsNullThenExpiryYearRequiredError() {
    PaymentRequest request = validRequest();
    request.setExpiryYear(null);
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.EXPIRY_YEAR_REQUIRED, result.getErrors().get(0).code());
  }

  // ── Expiry date (combined) ───────────────────────────────────────────────────

  @Test
  void whenExpiryDateIsInPastThenExpiryDateInPastError() {
    PaymentRequest request = validRequest();
    request.setExpiryMonth(1);
    request.setExpiryYear(2025);
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.EXPIRY_DATE_IN_PAST, result.getErrors().get(0).code());
  }

  @Test
  void whenExpiryDateIsCurrentMonthThenValid() {
    // Card expiring 03/2026 is valid through March 31, 2026
    PaymentRequest request = validRequest();
    request.setExpiryMonth(3);
    request.setExpiryYear(2026);
    assertTrue(PaymentRequestValidator.validate(request).isValid());
  }

  @Test
  void whenExpiryDateIsNextMonthThenValid() {
    PaymentRequest request = validRequest();
    request.setExpiryMonth(4);
    request.setExpiryYear(2026);
    assertTrue(PaymentRequestValidator.validate(request).isValid());
  }

  @Test
  void whenExpiryDateIsLastMonthThenExpiredError() {
    // Card expiring 02/2026 expired on March 1, 2026
    PaymentRequest request = validRequest();
    request.setExpiryMonth(2);
    request.setExpiryYear(2026);
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.EXPIRY_DATE_IN_PAST, result.getErrors().get(0).code());
  }

  // ── Currency ─────────────────────────────────────────────────────────────────

  @Test
  void whenCurrencyIsNullThenCurrencyRequiredError() {
    PaymentRequest request = validRequest();
    request.setCurrency(null);
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.CURRENCY_REQUIRED, result.getErrors().get(0).code());
  }

  @Test
  void whenCurrencyIsUnsupportedThenCurrencyNotSupportedError() {
    PaymentRequest request = validRequest();
    request.setCurrency("JPY");
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.CURRENCY_NOT_SUPPORTED, result.getErrors().get(0).code());
  }

  @Test
  void whenCurrencyIsNotThreeCharsThenInvalidLengthError() {
    PaymentRequest request = validRequest();
    request.setCurrency("US");
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.CURRENCY_INVALID_LENGTH, result.getErrors().get(0).code());
  }

  @Test
  void whenCurrencyIsUsdThenValid() {
    PaymentRequest request = validRequest();
    request.setCurrency("USD");
    assertTrue(PaymentRequestValidator.validate(request).isValid());
  }

  @Test
  void whenCurrencyIsGbpThenValid() {
    PaymentRequest request = validRequest();
    request.setCurrency("GBP");
    assertTrue(PaymentRequestValidator.validate(request).isValid());
  }

  @Test
  void whenCurrencyIsEurThenValid() {
    PaymentRequest request = validRequest();
    request.setCurrency("EUR");
    assertTrue(PaymentRequestValidator.validate(request).isValid());
  }

  // ── Amount ───────────────────────────────────────────────────────────────────

  @Test
  void whenAmountIsNullThenAmountRequiredError() {
    PaymentRequest request = validRequest();
    request.setAmount(null);
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.AMOUNT_REQUIRED, result.getErrors().get(0).code());
  }

  @Test
  void whenAmountIsZeroThenAmountInvalidError() {
    PaymentRequest request = validRequest();
    request.setAmount(0);
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.AMOUNT_INVALID, result.getErrors().get(0).code());
  }

  @Test
  void whenAmountIsNegativeThenAmountInvalidError() {
    PaymentRequest request = validRequest();
    request.setAmount(-1);
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.AMOUNT_INVALID, result.getErrors().get(0).code());
  }

  @Test
  void whenAmountIsPositiveThenValid() {
    PaymentRequest request = validRequest();
    request.setAmount(1);
    assertTrue(PaymentRequestValidator.validate(request).isValid());
  }

  @Test
  void whenAmountIsAtMaximumThenValid() {
    PaymentRequest request = validRequest();
    request.setAmount(PaymentRequestValidator.MAX_AMOUNT);
    assertTrue(PaymentRequestValidator.validate(request).isValid());
  }

  @Test
  void whenAmountExceedsMaximumThenAmountTooLargeError() {
    PaymentRequest request = validRequest();
    request.setAmount(PaymentRequestValidator.MAX_AMOUNT + 1);
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.AMOUNT_TOO_LARGE, result.getErrors().get(0).code());
  }

  // ── CVV ──────────────────────────────────────────────────────────────────────

  @Test
  void whenCvvIsNullThenCvvRequiredError() {
    PaymentRequest request = validRequest();
    request.setCvv(null);
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.CVV_REQUIRED, result.getErrors().get(0).code());
  }

  @Test
  void whenCvvIsTwoCharsThenCvvInvalidLengthError() {
    PaymentRequest request = validRequest();
    request.setCvv("12");
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.CVV_INVALID_LENGTH, result.getErrors().get(0).code());
  }

  @Test
  void whenCvvIsFiveCharsThenCvvInvalidLengthError() {
    PaymentRequest request = validRequest();
    request.setCvv("12345");
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.CVV_INVALID_LENGTH, result.getErrors().get(0).code());
  }

  @Test
  void whenCvvContainsLettersThenCvvNonNumericError() {
    PaymentRequest request = validRequest();
    request.setCvv("abc");
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(ValidationErrorCode.CVV_NON_NUMERIC, result.getErrors().get(0).code());
  }

  @Test
  void whenCvvIsThreeDigitsThenValid() {
    PaymentRequest request = validRequest();
    request.setCvv("123");
    assertTrue(PaymentRequestValidator.validate(request).isValid());
  }

  @Test
  void whenCvvIsFourDigitsThenValid() {
    PaymentRequest request = validRequest();
    request.setCvv("1234");
    assertTrue(PaymentRequestValidator.validate(request).isValid());
  }

  @Test
  void whenCvvHasLeadingZerosThenValid() {
    PaymentRequest request = validRequest();
    request.setCvv("007");
    assertTrue(PaymentRequestValidator.validate(request).isValid());
  }

  // ── Multiple errors ──────────────────────────────────────────────────────────

  @Test
  void whenMultipleFieldsInvalidThenAllErrorsReturned() {
    PaymentRequest request = validRequest();
    request.setCardNumber("123"); // too short
    request.setCurrency("JPY");   // unsupported
    request.setAmount(0);         // invalid
    ValidationResult result = PaymentRequestValidator.validate(request);
    assertFalse(result.isValid());
    assertEquals(3, result.getErrors().size());
  }
}
