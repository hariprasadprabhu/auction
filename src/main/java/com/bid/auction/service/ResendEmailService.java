package com.bid.auction.service;

import com.bid.auction.exception.EmailDeliveryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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

    /**
     * Sends a plain-text email.
     *
     * @param to      recipient address
     * @param subject email subject
     * @param text    plain-text body
     * @throws EmailDeliveryException if the Resend API returns an error
     */
    public void sendText(String to, String subject, String text) {
        if (apiKey == null || apiKey.isBlank()) {
            // Local dev: no key configured — just log so developers can see the OTP.
            log.warn("RESEND_API_KEY is not set — email will NOT be delivered to {}", to);
            log.info("[DEV EMAIL]\nTo:      {}\nSubject: {}\n\n{}", to, subject, text);
            return;
        }

        Map<String, Object> payload = Map.of(
                "from", fromAddress,
                "to",   List.of(to),
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

            log.info("Email dispatched via Resend to {}", to);

        } catch (RestClientException e) {
            log.error("Resend API call failed for {}: {}", to, e.getMessage());
            throw new EmailDeliveryException("Could not send email at this time", e);
        }
    }
}

