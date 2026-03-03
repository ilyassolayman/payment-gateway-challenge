package com.checkout.payment.gateway.controller;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.IPaymentsRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  IPaymentsRepository paymentsRepository;
  @MockBean
  RestTemplate restTemplate;

  // ── GET /payment/{id} ──────────────────────────────────────────────────────

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PostPaymentResponse payment = PostPaymentResponse.builder()
        .id(UUID.randomUUID())
        .amount(10)
        .currency("USD")
        .status(PaymentStatus.AUTHORIZED)
        .expiryMonth(12)
        .expiryYear(2024)
        .cardNumberLastFour("4321")
        .build();

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorMessage").value(org.hamcrest.Matchers.containsString("Payment not found")))
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }

  // ── POST /payment ───────────────────────────────────────────────────────────

  @Test
  void whenValidPaymentWithOddEndingCardThenAuthorizedResponseReturned() throws Exception {
    when(restTemplate.postForEntity(anyString(), any(), eq(BankPaymentResponse.class)))
        .thenReturn(ResponseEntity.ok(new BankPaymentResponse(true, "auth-code-123")));

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "2222405343248877",
                  "expiry_month": 4,
                  "expiry_year": 2027,
                  "currency": "GBP",
                  "amount": 100,
                  "cvv": "123"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value("8877"))
        .andExpect(jsonPath("$.expiryMonth").value(4))
        .andExpect(jsonPath("$.expiryYear").value(2027))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(100))
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.rejectionReasons").doesNotExist())
        .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/payment/")));
  }

  @Test
  void whenValidPaymentWithEvenEndingCardThenDeclinedResponseReturned() throws Exception {
    when(restTemplate.postForEntity(anyString(), any(), eq(BankPaymentResponse.class)))
        .thenReturn(ResponseEntity.ok(new BankPaymentResponse(false, "")));

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "2222405343248112",
                  "expiry_month": 6,
                  "expiry_year": 2027,
                  "currency": "USD",
                  "amount": 500,
                  "cvv": "456"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Declined"))
        .andExpect(jsonPath("$.cardNumberLastFour").value("8112"))
        .andExpect(jsonPath("$.id").isNotEmpty());
  }

  @Test
  void whenCardNumberTooShortThenRejectedWith400() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "1234567890123",
                  "expiry_month": 4,
                  "expiry_year": 2027,
                  "currency": "GBP",
                  "amount": 100,
                  "cvv": "123"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.rejectionReasons").isNotEmpty());
  }

  @Test
  void whenCardNumberContainsLettersThenRejectedWith400() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "222240534324abcd",
                  "expiry_month": 4,
                  "expiry_year": 2027,
                  "currency": "GBP",
                  "amount": 100,
                  "cvv": "123"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.rejectionReasons").isNotEmpty());
  }

  @Test
  void whenCardExpiredThenRejectedWith400() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "2222405343248877",
                  "expiry_month": 2,
                  "expiry_year": 2026,
                  "currency": "GBP",
                  "amount": 100,
                  "cvv": "123"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.rejectionReasons").isNotEmpty());
  }

  @Test
  void whenUnsupportedCurrencyThenRejectedWith400() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "2222405343248877",
                  "expiry_month": 4,
                  "expiry_year": 2027,
                  "currency": "JPY",
                  "amount": 100,
                  "cvv": "123"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.rejectionReasons").isNotEmpty());
  }

  @Test
  void whenInvalidCvvThenRejectedWith400() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "2222405343248877",
                  "expiry_month": 4,
                  "expiry_year": 2027,
                  "currency": "GBP",
                  "amount": 100,
                  "cvv": "ab"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.rejectionReasons").isNotEmpty());
  }

  @Test
  void whenAmountIsZeroThenRejectedWith400() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "2222405343248877",
                  "expiry_month": 4,
                  "expiry_year": 2027,
                  "currency": "GBP",
                  "amount": 0,
                  "cvv": "123"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.rejectionReasons").isNotEmpty());
  }

  @Test
  void whenCardEndsInZeroThenBankReturns503AndBadGatewayReturned() throws Exception {
    when(restTemplate.postForEntity(anyString(), any(), eq(BankPaymentResponse.class)))
        .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "2222405343248870",
                  "expiry_month": 4,
                  "expiry_year": 2027,
                  "currency": "GBP",
                  "amount": 100,
                  "cvv": "123"
                }
                """))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.errorMessage").value("Bank payment service is unavailable"));
  }

  @Test
  void whenBankIsUnreachableThenBadGatewayReturned() throws Exception {
    when(restTemplate.postForEntity(anyString(), any(), eq(BankPaymentResponse.class)))
        .thenThrow(new ResourceAccessException("Connection refused"));

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "2222405343248877",
                  "expiry_month": 4,
                  "expiry_year": 2027,
                  "currency": "GBP",
                  "amount": 100,
                  "cvv": "123"
                }
                """))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.errorMessage").value("Bank payment service is unavailable"));
  }

  // ── HTTP protocol error scenarios ───────────────────────────────────────────

  @Test
  void whenMalformedJsonThenBadRequestReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{this is not valid json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorMessage").value("Malformed JSON request"));
  }

  @Test
  void whenUnsupportedHttpMethodThenMethodNotAllowedReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.delete("/payment"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.errorMessage").value("HTTP method not supported"));
  }

  @Test
  void whenInvalidPaymentIdFormatThenBadRequestReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorMessage").value("Invalid request parameter: id"));
  }

  @Test
  void whenUnsupportedContentTypeThenUnsupportedMediaTypeReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.TEXT_PLAIN)
            .content("card_number=1234"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.errorMessage").value("Content type not supported. Use application/json"));
  }

  @Test
  void whenUnknownEndpointThenNotFoundReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/unknown/endpoint"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorMessage").value(org.hamcrest.Matchers.containsString("No endpoint found for")));
  }
}
