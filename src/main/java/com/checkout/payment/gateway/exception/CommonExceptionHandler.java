package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePaymentNotFoundException(PaymentNotFoundException ex) {
    LOG.warn("Payment not found: {}", ex.getMessage());
    return new ResponseEntity<>(new ErrorResponse(ex.getMessage()), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
    LOG.warn("Payment rejected: {}", ex.getMessage());
    return new ResponseEntity<>(new ErrorResponse(ex.getMessage()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(BankPaymentClientException.class)
  public ResponseEntity<ErrorResponse> handleBankPaymentClientException(BankPaymentClientException ex) {
    LOG.error("Bank rejected payment request — mapping error: {}", ex.getMessage());
    return new ResponseEntity<>(new ErrorResponse(ex.getMessage()), HttpStatus.BAD_GATEWAY);
  }

  @ExceptionHandler(BankPaymentRequestException.class)
  public ResponseEntity<ErrorResponse> handleBankPaymentRequestException(BankPaymentRequestException ex) {
    LOG.error("Bank payment request failed: {}", ex.getMessage());
    return new ResponseEntity<>(new ErrorResponse(ex.getMessage()), HttpStatus.BAD_GATEWAY);
  }

  @ExceptionHandler(BankPaymentResponseException.class)
  public ResponseEntity<ErrorResponse> handleBankPaymentResponseException(BankPaymentResponseException ex) {
    LOG.error("Bank payment response invalid: {}", ex.getMessage());
    return new ResponseEntity<>(new ErrorResponse(ex.getMessage()), HttpStatus.BAD_GATEWAY);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
    LOG.warn("Malformed request body: {}", ex.getMessage());
    return new ResponseEntity<>(new ErrorResponse("Malformed JSON request"), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
    LOG.warn("HTTP method not supported: {}", ex.getMethod());
    return new ResponseEntity<>(new ErrorResponse("HTTP method not supported"), HttpStatus.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    LOG.warn("Invalid path variable '{}': {}", ex.getName(), ex.getMessage());
    return new ResponseEntity<>(new ErrorResponse("Invalid request parameter: " + ex.getName()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
    LOG.warn("Unsupported media type: {}", ex.getContentType());
    return new ResponseEntity<>(new ErrorResponse("Content type not supported. Use application/json"), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex) {
    LOG.warn("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());
    return new ResponseEntity<>(new ErrorResponse("No endpoint found for " + ex.getRequestURL()), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
    LOG.error("Unexpected error", ex);
    return new ResponseEntity<>(new ErrorResponse("An unexpected error occurred"), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
