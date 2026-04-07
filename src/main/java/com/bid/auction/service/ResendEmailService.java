package com.bid.auction.service;

import com.bid.auction.exception.EmailDeliveryException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Sends transactional email via the Resend HTTP API (https://resend.com).
 * Uses HTTPS port 443 — works on Railway, Render, and any other cloud platform
 * that blocks outbound SMTP (ports 25 / 465 / 587).
 *
 * <p>Required environment variables:
 * <ul>
 *   <li>{@code RESEND_API_KEY} – API key from resend.com</li>
 *   <li>{@code RESEND_FROM}    – Verified sender address, e.g. {@code noreply@bidplayers.in}</li>
 * </ul>
 *
 * <p>When {@code RESEND_API_KEY} is not set (local dev), emails are only logged.
 */
@Slf4j
@Service
public class ResendEmailService {

    private final String apiKey;
    private final String fromAddress;
    private final RestClient restClient;

    public ResendEmailService(
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from:noreply@bidplayers.in}") String fromAddress) {
        this.apiKey      = apiKey;
        this.fromAddress = fromAddress;
        this.restClient  = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .build();
    }

    @PostConstruct
    void logConfig() {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("⚠️  RESEND_API_KEY is NOT set — all email sending will fail with 503!");
        } else {
            log.info("✅ Resend email service configured. Sending from: {}", fromAddress);
        }
    }

    /**
     * Sends a plain-text email.
     *
     * @param to      recipient address
     * @param subject email subject
     * @param text    plain-text body
     * @throws EmailDeliveryException if the Resend API returns an error
     */
    public void sendText(String to, String subject, String text) {

        // Surface misconfiguration immediately rather than silently dropping emails
        if (apiKey == null || apiKey.isBlank()) {
            throw new EmailDeliveryException(
                    "Email service is not configured on this server (RESEND_API_KEY missing). " +
                    "Please contact support.", null);
        }

        Map<String, Object> payload = Map.of(
                "from",    fromAddress,
                "to",      List.of(to),
                "subject", subject,
                "text",    text
        );

        try {
            restClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("✉️  Email dispatched via Resend → {}", to);

        } catch (RestClientResponseException e) {
            // Read the response body so we know exactly what Resend rejected
            String resendError = e.getResponseBodyAsString();
            log.error("Resend API error (HTTP {}): {}", e.getStatusCode(), resendError);
            throw new EmailDeliveryException(
                    "Email delivery failed (Resend HTTP " + e.getStatusCode() + "): " + resendError, e);

        } catch (Exception e) {
            log.error("Resend API call failed for {}: {}", to, e.getMessage());
            throw new EmailDeliveryException("Email delivery failed: " + e.getMessage(), e);
        }
    }
}
