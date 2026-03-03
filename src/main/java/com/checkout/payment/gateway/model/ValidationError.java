package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.ValidationErrorCode;

public record ValidationError(ValidationErrorCode code, String message) {}
