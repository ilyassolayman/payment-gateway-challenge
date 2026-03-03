package com.checkout.payment.gateway.integration;


import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerIntegrationTest {

  private static final String BASE_URL_V1 = "/api/v1";
  private static final String PAYMENT = "/payment";

  @Autowired
  private MockMvc mvc;
  @Autowired
  private RestTemplate restTemplate;
  @Value("${bank.payment.url}")
  private String bankPaymentUrl;

  private MockRestServiceServer mockBankServer;

  @BeforeEach
  void setUp() {
    mockBankServer = MockRestServiceServer.createServer(restTemplate);
  }

  // ── GET /payment/{id} ──────────────────────────────────────────────────────

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    mockBankServer.expect(requestTo(bankPaymentUrl))
        .andRespond(withSuccess("""
            {"authorized":true,"authorization_code":"auth-code-123"}
            """, MediaType.APPLICATION_JSON));

    String location = mvc.perform(MockMvcRequestBuilders.post(BASE_URL_V1 + PAYMENT)
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
        .andReturn()
        .getResponse()
        .getHeader("Location");

    mvc.perform(MockMvcRequestBuilders.get(URI.create(location).getPath()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value("8877"))
        .andExpect(jsonPath("$.expiryMonth").value(4))
        .andExpect(jsonPath("$.expiryYear").value(2027))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(100));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get(BASE_URL_V1 + PAYMENT + "/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorMessage").value(org.hamcrest.Matchers.containsString("Payment not found")))
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }

  // ── POST /payment ───────────────────────────────────────────────────────────

  @Test
  void whenValidPaymentWithOddEndingCardThenAuthorizedResponseReturned() throws Exception {
    mockBankServer.expect(requestTo(bankPaymentUrl))
        .andRespond(withSuccess("""
            {"authorized":true,"authorization_code":"auth-code-123"}
            """, MediaType.APPLICATION_JSON));

    mvc.perform(MockMvcRequestBuilders.post(BASE_URL_V1 + PAYMENT)
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
        .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(PAYMENT + "/")));
  }

  @Test
  void whenValidPaymentWithEvenEndingCardThenDeclinedResponseReturned() throws Exception {
    mockBankServer.expect(requestTo(bankPaymentUrl))
        .andRespond(withSuccess("""
            {"authorized":false,"authorization_code":""}
            """, MediaType.APPLICATION_JSON));

    mvc.perform(MockMvcRequestBuilders.post(BASE_URL_V1 + PAYMENT)
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
  void whenSingleFieldInvalidThenRejectedWith400AndOneReason() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post(BASE_URL_V1 + PAYMENT)
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
        .andExpect(jsonPath("$.rejectionReasons.length()").value(1));
  }

  @Test
  void whenMultipleFieldsInvalidThenRejectedWith400AndAllReasonsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post(BASE_URL_V1 + PAYMENT)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "1234567890123",
                  "expiry_month": 4,
                  "expiry_year": 2027,
                  "currency": "JPY",
                  "amount": 0,
                  "cvv": "123"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.rejectionReasons.length()").value(3));
  }

  @Test
  void whenBankServiceUnavailableThenBadGatewayReturned() throws Exception {
    mockBankServer.expect(requestTo(bankPaymentUrl))
        .andRespond(withServiceUnavailable());

    mvc.perform(MockMvcRequestBuilders.post(BASE_URL_V1 + PAYMENT)
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

  // ── Exception handler scenarios ─────────────────────────────────────────────

  @Test
  void whenMalformedJsonThenBadRequestReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post(BASE_URL_V1 + PAYMENT)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{this is not valid json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorMessage").value("Malformed JSON request"));
  }

  @Test
  void whenUnsupportedHttpMethodThenMethodNotAllowedReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.delete(BASE_URL_V1 + PAYMENT))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.errorMessage").value("HTTP method not supported"));
  }

  @Test
  void whenInvalidPaymentIdFormatThenBadRequestReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get(BASE_URL_V1 + PAYMENT + "/not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorMessage").value("Invalid request parameter: id"));
  }

  @Test
  void whenUnsupportedContentTypeThenUnsupportedMediaTypeReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post(BASE_URL_V1 + PAYMENT)
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
