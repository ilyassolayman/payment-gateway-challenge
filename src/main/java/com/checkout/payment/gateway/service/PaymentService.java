package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.mapper.PaymentMapper;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.utils.PaymentRequestValidator;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
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

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new PaymentNotFoundException(id));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest request) {
    ValidationResult validationResult = PaymentRequestValidator.validate(request);

    if (!validationResult.isValid()) {
      LOG.warn("Payment rejected due to validation errors: {}", validationResult.getErrors());
      return PostPaymentResponse.builder()
          .status(PaymentStatus.REJECTED)
          .rejectionReasons(validationResult.getErrors().stream()
              .map(ValidationError::message)
              .collect(Collectors.toList()))
          .build();
    }

    BankPaymentResponse bankResponse = bankPaymentService.processPayment(PaymentMapper.toBankPaymentRequest(request));
    PostPaymentResponse response = PaymentMapper.toPostPaymentResponse(request, bankResponse);

    paymentsRepository.add(response);
    LOG.debug("Payment {} processed with status {}", response.getId(), response.getStatus());
    return response;
  }
}
