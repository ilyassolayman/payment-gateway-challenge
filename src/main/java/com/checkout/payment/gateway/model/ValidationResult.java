package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.ValidationErrorCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationResult {

  private final List<ValidationError> errors = new ArrayList<>();

  public void addError(ValidationErrorCode code, String message) {
    errors.add(new ValidationError(code, message));
  }

  public boolean isValid() {
    return errors.isEmpty();
  }

  public List<ValidationError> getErrors() {
    return Collections.unmodifiableList(errors);
  }
}
