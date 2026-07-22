package gg.renz.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.renz.DataTransferObjects.TurnstileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class CaptchaService
{
    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    public boolean verifyCaptcha(String token)
    {
        if (token == null || token.isEmpty())
        {
            return false;
        }
        log.info("token: {}", token.substring(0, 10));

        String secretKey = System.getenv("TURNSTILE_SECRET_KEY");
        log.info("key found: {}", secretKey.substring(0, 4));

        ObjectMapper mapper = new ObjectMapper();

        try
        {
            var formData = String.format(
                    "secret=%s&response=%s",
                    URLEncoder.encode(secretKey, StandardCharsets.UTF_8),
                    URLEncoder.encode(token, StandardCharsets.UTF_8)
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Turnstile response: {}", response.body());

            TurnstileResponse output = mapper.readValue(response.body(), TurnstileResponse.class);

            return output.success();

        } catch (Exception e)
        {
            log.error("Error de cloudflare", e);
            return false;
        }
    }

}
