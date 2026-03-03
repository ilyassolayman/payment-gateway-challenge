package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.exception.BankPaymentClientException;
import com.checkout.payment.gateway.exception.BankPaymentRequestException;
import com.checkout.payment.gateway.exception.BankPaymentResponseException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class BankPaymentService implements IBankPaymentService {

  private static final Logger LOG = LoggerFactory.getLogger(BankPaymentService.class);

  private final RestTemplate restTemplate;
  private final String bankPaymentUrl;

  public BankPaymentService(RestTemplate restTemplate,
      @Value("${bank.payment.url}") String bankPaymentUrl) {
    this.restTemplate = restTemplate;
    this.bankPaymentUrl = bankPaymentUrl;
  }

  @Override
  public BankPaymentResponse processPayment(BankPaymentRequest request) {
    LOG.info("Sending payment request to bank at {}", bankPaymentUrl);
    BankPaymentResponse bankResponse;
    try {
      ResponseEntity<BankPaymentResponse> responseEntity =
          restTemplate.postForEntity(bankPaymentUrl, request, BankPaymentResponse.class);
      LOG.info("Received response from bank with status {}", responseEntity.getStatusCode());
      bankResponse = responseEntity.getBody();
    } catch (HttpClientErrorException e) {
      LOG.error("Bank rejected request — possible mapping error. Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new BankPaymentClientException("Bank rejected the payment request");
    } catch (HttpServerErrorException e) {
      LOG.error("Bank returned server error. Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new BankPaymentRequestException("Bank payment service is unavailable");
    } catch (RestClientException e) {
      LOG.error("Bank unreachable: {}", e.getMessage());
      throw new BankPaymentRequestException("Bank payment service is unavailable");
    }

    if (bankResponse == null) {
      LOG.error("Bank payment returned empty response body");
      throw new BankPaymentResponseException("Bank payment service returned an invalid response");
    }

    LOG.info("Bank payment response — authorized: {}", bankResponse.isAuthorized());
    return bankResponse;
  }
}
