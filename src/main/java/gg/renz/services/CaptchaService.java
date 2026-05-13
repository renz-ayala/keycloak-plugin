package gg.renz.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.renz.DataTransferObjects.TurnstileRequest;
import gg.renz.DataTransferObjects.TurnstileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CaptchaService
{
    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    public boolean verifyCaptcha(String token)
    {
        if (token == null || token.isEmpty()) return false;

        ObjectMapper mapper = new ObjectMapper();
        TurnstileRequest input = new TurnstileRequest("0x4AAAAAAB76DAPfJazllHj1fU9e4Xqhnpw", token);

        try
        {
            String inputJson = mapper.writeValueAsString(input);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(inputJson))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            TurnstileResponse output = mapper.readValue(response.body(), TurnstileResponse.class);

            return output.success();

        } catch (Exception e)
        {
            log.error("Error de cloudflare", e);
            return false;
        }
    }

}
