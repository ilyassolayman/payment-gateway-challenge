package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.service.IPaymentService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1")
public class PaymentGatewayController {

  private final IPaymentService paymentService;

  public PaymentGatewayController(IPaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping("/payment")
  public ResponseEntity<PaymentResponse> postPayment(@RequestBody PaymentRequest request) {
    PaymentResponse response = paymentService.processPayment(request);
    if (response.getStatus() == PaymentStatus.REJECTED) {
      return ResponseEntity.badRequest().body(response);
    }
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(response.getId())
        .toUri();
    return ResponseEntity.created(location).body(response);
  }

  @GetMapping("/payment/{id}")
  public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable UUID id) {
    return new ResponseEntity<>(paymentService.getPaymentById(id), HttpStatus.OK);
  }
}
