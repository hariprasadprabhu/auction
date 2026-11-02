package com.bid.auth.service;

import com.bid.auth.exception.EmailDeliveryException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ResendEmailService {

    private final String apiKey;
    private final String fromAddress;
    private final RestClient restClient;

    public ResendEmailService(
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from:noreply@bidplayers.in}") String fromAddress) {
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
        this.restClient = RestClient.builder().baseUrl("https://api.resend.com").build();
    }

    @PostConstruct
    void logConfig() {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("RESEND_API_KEY is not set; email sending is unavailable.");
        } else {
            log.info("Resend email service configured. Sending from: {}", fromAddress);
        }
    }

    public void sendText(String to, String subject, String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new EmailDeliveryException(
                    "Email service is not configured on this server (RESEND_API_KEY missing). Please contact support.",
                    null);
        }

        Map<String, Object> payload = Map.of(
                "from", fromAddress,
                "to", List.of(to),
                "subject", subject,
                "text", text);

        try {
            restClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email dispatched via Resend to {}", to);
        } catch (RestClientResponseException e) {
            log.error("Resend API error (HTTP {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new EmailDeliveryException(
                    "Email delivery failed (Resend HTTP " + e.getStatusCode() + ").", e);
        } catch (RestClientException e) {
            log.error("Resend API call failed for {}: {}", to, e.getMessage());
            throw new EmailDeliveryException("Email delivery failed.", e);
        }
    }
}
