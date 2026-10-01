package com.example.loot.Service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class EmailServiceTests {
    @Test void sendsPlainTextOverHttps() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var service = new EmailService(builder.build(), "test-key", "loot@example.test", "Loot");
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                    {"sender":{"name":"Loot","email":"loot@example.test"},"to":[{"email":"qa@example.test"}],
                     "subject":"Test","textContent":"مرحبا\nHello"}
                    """.replace("مرحبا\nHello", "مرحبا\\nHello")))
                .andRespond(withStatus(HttpStatus.CREATED).body("{\"messageId\":\"email-test-id\"}").contentType(MediaType.APPLICATION_JSON));
        service.sendEmail("qa@example.test", "Test", "مرحبا\nHello");
        server.verify();
    }

    @Test void missingConfigurationFailsOnlyWhenSending() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        for (String[] config : new String[][] {{"", "sender@example.test"}, {"test-key", ""}}) {
            var service = assertDoesNotThrow(() -> new EmailService(builder.build(), config[0], config[1], "Loot"));
            assertThrows(EmailDeliveryException.class, () -> service.sendEmail("qa@example.test", "Test", "Body"));
        }
        server.verify();
    }

    @Test void providerFailuresAreSanitized() {
        for (var status : new HttpStatus[] {HttpStatus.UNAUTHORIZED, HttpStatus.UNPROCESSABLE_ENTITY,
                HttpStatus.TOO_MANY_REQUESTS, HttpStatus.INTERNAL_SERVER_ERROR}) {
            var builder = RestClient.builder();
            var server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                    .andRespond(withStatus(status).body("secret-provider-detail"));
            var service = new EmailService(builder.build(), "test-key", "sender@example.test", "Loot");
            var error = assertThrows(EmailDeliveryException.class, () -> service.sendEmail("qa@example.test", "Test", "Body"));
            assertFalse(error.toString().contains("secret-provider-detail"));
            assertNull(error.getCause());
            server.verify();
        }
    }

    @Test void networkFailureAndMissingReceiptAreNotSuccess() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email")).andRespond(withException(new IOException("secret-detail")));
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        var service = new EmailService(builder.build(), "test-key", "sender@example.test", "Loot");
        assertThrows(EmailDeliveryException.class, () -> service.sendEmail("qa@example.test", "Test", "Body"));
        assertThrows(EmailDeliveryException.class, () -> service.sendEmail("qa@example.test", "Test", "Body"));
        server.verify();
    }
}
