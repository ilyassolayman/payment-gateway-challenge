package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.ValidationErrorCode;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.ValidationResult;
import java.time.YearMonth;
import java.util.Set;

public class PaymentRequestValidator {

  private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "GBP", "EUR");

  private PaymentRequestValidator() {}

  public static ValidationResult validate(PostPaymentRequest request) {
    ValidationResult result = new ValidationResult();
    validateCardNumber(request.getCardNumber(), result);
    validateExpiryMonth(request.getExpiryMonth(), result);
    validateExpiryDate(request.getExpiryYear(), request.getExpiryMonth(), result);
    validateCurrency(request.getCurrency(), result);
    validateAmount(request.getAmount(), result);
    validateCvv(request.getCvv(), result);
    return result;
  }

  private static void validateCardNumber(String cardNumber, ValidationResult result) {
    if (cardNumber == null || cardNumber.isBlank()) {
      result.addError(ValidationErrorCode.CARD_NUMBER_REQUIRED, "Card number is required");
      return;
    }
    if (cardNumber.length() < 14 || cardNumber.length() > 19) {
      result.addError(ValidationErrorCode.CARD_NUMBER_INVALID_LENGTH, "Card number must be between 14 and 19 digits");
    }
    if (!cardNumber.chars().allMatch(Character::isDigit)) {
      result.addError(ValidationErrorCode.CARD_NUMBER_NON_NUMERIC, "Card number must contain only numeric characters");
    }
  }

  private static void validateExpiryMonth(int expiryMonth, ValidationResult result) {
    if (expiryMonth < 1 || expiryMonth > 12) {
      result.addError(ValidationErrorCode.EXPIRY_MONTH_INVALID, "Expiry month must be between 1 and 12");
    }
  }

  private static void validateExpiryDate(int expiryYear, int expiryMonth, ValidationResult result) {
    try {
      YearMonth expiry = YearMonth.of(expiryYear, expiryMonth);
      if (expiry.isBefore(YearMonth.now())) {
        result.addError(ValidationErrorCode.EXPIRY_DATE_IN_PAST, "Card has expired");
      }
    } catch (Exception e) {
      result.addError(ValidationErrorCode.EXPIRY_DATE_INVALID, "Invalid expiry date");
    }
  }

  private static void validateCurrency(String currency, ValidationResult result) {
    if (currency == null || currency.isBlank()) {
      result.addError(ValidationErrorCode.CURRENCY_REQUIRED, "Currency is required");
      return;
    }
    if (currency.length() != 3) {
      result.addError(ValidationErrorCode.CURRENCY_INVALID_LENGTH, "Currency must be 3 characters");
      return;
    }
    if (!SUPPORTED_CURRENCIES.contains(currency.toUpperCase())) {
      result.addError(ValidationErrorCode.CURRENCY_NOT_SUPPORTED, "Currency must be one of: USD, GBP, EUR");
    }
  }

  private static void validateAmount(int amount, ValidationResult result) {
    if (amount <= 0) {
      result.addError(ValidationErrorCode.AMOUNT_INVALID, "Amount must be greater than zero");
    }
  }

  private static void validateCvv(String cvv, ValidationResult result) {
    if (cvv == null || cvv.isBlank()) {
      result.addError(ValidationErrorCode.CVV_REQUIRED, "CVV is required");
      return;
    }
    if (cvv.length() < 3 || cvv.length() > 4) {
      result.addError(ValidationErrorCode.CVV_INVALID_LENGTH, "CVV must be 3 or 4 characters");
    }
    if (!cvv.chars().allMatch(Character::isDigit)) {
      result.addError(ValidationErrorCode.CVV_NON_NUMERIC, "CVV must contain only numeric characters");
    }
  }
}
