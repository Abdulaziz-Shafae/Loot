package com.example.loot.Service;

import com.example.loot.DTO.LowStockDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private final RestClient client;
    private final String apiKey;
    private final String from;
    private final String fromName;

    @Autowired
    public EmailService(@Value("${brevo.api.key:}") String apiKey,
                        @Value("${brevo.from.email:}") String from,
                        @Value("${brevo.from.name:Loot}") String fromName) {
        this(defaultClient(), apiKey, from, fromName);
    }

    EmailService(RestClient client, String apiKey, String from, String fromName) {
        this.client = client;
        this.apiKey = apiKey;
        this.from = from;
        this.fromName = fromName.isBlank() ? "Loot" : fromName;
    }

    private static RestClient defaultClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return RestClient.builder().requestFactory(factory).build();
    }

    public void sendEmail(String to, String subject, String body) {
        if (apiKey.isBlank() || from.isBlank()) {
            throw new EmailDeliveryException("Email delivery is not configured");
        }
        try {
            var response = client.post().uri("https://api.brevo.com/v3/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("sender", Map.of("name", fromName, "email", from),
                            "to", List.of(Map.of("email", to)), "subject", subject, "textContent", body))
                    .retrieve().body(BrevoResponse.class);
            if (response == null || response.messageId() == null || response.messageId().isBlank()) {
                throw new EmailDeliveryException("Email provider returned no message ID");
            }
        } catch (RestClientResponseException e) {
            // Never log provider response bodies, request headers, recipients or reset codes.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Email provider rejected request (HTTP {})", e.getStatusCode().value());
            throw new EmailDeliveryException("Email provider rejected the request");
        } catch (RestClientException e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Email provider request failed");
            throw new EmailDeliveryException("Email provider is unavailable");
        }
    }

    private record BrevoResponse(String messageId) {}

    public void sendWelcomeEmail(String toEmail, String name) {

        sendEmail(toEmail, "Welcome to Loot!",
                "Hi " + name + ",\n\n" +
                        "Welcome to Loot!\n\n" +
                        "Your account has been created successfully.\n" +
                        "You can now start managing your pantry and discovering recipes.\n\n" +
                        "Enjoy cooking!\n" +
                        "Loot Team"
        );

    }

    public void sendVerificationCode(String toEmail, String name, Integer code) {

        sendEmail(toEmail, "Loot Password Reset Code",
                "Hi " + name + ",\n\n" +
                        "We received a request to reset your Loot password.\n\n" +
                        "Your verification code is:\n\n" +
                        code + "\n\n" +
                        "Use this code to reset your password.\n" +
                        "If you did not request a password reset, you can ignore this email.\n\n" +
                        "Loot Team"
        );

    }

    public void sendLowStockEmail(String toEmail, String name, List<LowStockDTO> list) {

        StringBuilder lowStockList = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {

            lowStockList.append(list.get(i).getName())
                    .append("\nAvailable: ")
                    .append(list.get(i).getAvailable())
                    .append("\nLow Stock Threshold: ")
                    .append(list.get(i).getThreshold())
                    .append("\nNeed to buy: ")
                    .append(list.get(i).getNeedToBuy())
                    .append("\n\n");
        }

        sendEmail(toEmail, "Low Stock List",
                "Hi " + name + ",\n\n" +
                        "The following ingredients in your pantry are running low:\n\n" +
                        lowStockList +
                        "Consider restocking these ingredients soon.\n\n" +
                        "Loot Team"
        );

    }


}
