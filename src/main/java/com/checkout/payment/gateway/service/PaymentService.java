package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.mapper.PaymentMapper;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.utils.PaymentRequestValidator;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.ValidationError;
import com.checkout.payment.gateway.model.ValidationResult;
import com.checkout.payment.gateway.repository.IPaymentsRepository;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentService implements IPaymentService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

  private final IPaymentsRepository paymentsRepository;
  private final IBankPaymentService bankPaymentService;

  public PaymentService(IPaymentsRepository paymentsRepository,
      IBankPaymentService bankPaymentService) {
    this.paymentsRepository = paymentsRepository;
    this.bankPaymentService = bankPaymentService;
  }

  public PaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to payment with ID {}", id);
    Payment payment = paymentsRepository.get(id).orElseThrow(() -> new PaymentNotFoundException(id));
    return PaymentMapper.toPaymentResponse(payment);
  }

  public PaymentResponse processPayment(PaymentRequest request) {
    request.setCardNumber(request.getCardNumber() == null ? null : request.getCardNumber().replaceAll("\\s", ""));
    ValidationResult validationResult = PaymentRequestValidator.validate(request);

    if (!validationResult.isValid()) {
      LOG.warn("Payment rejected due to validation errors: {}", validationResult.getErrors());
      return PaymentResponse.builder()
          .status(PaymentStatus.REJECTED)
          .rejectionReasons(validationResult.getErrors().stream()
              .map(ValidationError::message)
              .collect(Collectors.toList()))
          .build();
    }

    BankPaymentResponse bankResponse = bankPaymentService.processPayment(PaymentMapper.toBankPaymentRequest(request));
    Payment payment = PaymentMapper.toPayment(request, bankResponse);

    paymentsRepository.add(payment);
    LOG.debug("Payment {} processed with status {}", payment.getId(), payment.getStatus());
    return PaymentMapper.toPaymentResponse(payment);
  }
}
