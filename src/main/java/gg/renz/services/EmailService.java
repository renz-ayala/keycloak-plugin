package gg.renz.services;

import gg.renz.DataTransferObjects.ApiResponse;
import gg.renz.persistence.DataAccess;
import jakarta.mail.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;

public class EmailService
{
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final String SMTP_PASSWORD = System.getenv("SMTP_PASSWORD");
    private final String SMTP_FROM = System.getenv("SMTP_FROM");
    private final String DEPLOY = System.getenv("DEPLOY");

    DataAccess dataAccess = new DataAccess();

    private void send(String to, String body)
    {
        try {
            String sanitizedBody = body.replace("\"", "\\\"").replace("\n", "").replace("\r", "");

            String jsonBody = """
                {
                  "from": "%s",
                  "to": ["%s"],
                  "subject": "Código de verificación",
                  "html": "%s"
                }
                """.formatted(SMTP_FROM, to, sanitizedBody);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + SMTP_PASSWORD)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Correo enviado exitosamente vía API REST a: {}", to);
            } else {
                log.error("Error respuesta de Resend API [{}]: {}", response.statusCode(), response.body());
                throw new RuntimeException("Resend API rehusó el correo: " + response.body());
            }

        } catch (Exception e)
        {
            log.error("Error enviando email vía HTTP", e);
            throw new RuntimeException("Error enviando email", e);
        }
    }

    private String getVerifyCode()
    {
        int m = (int) Math.pow(10, 5);
        return String.valueOf(m + new Random().nextInt(9 * m));
    }

    public ApiResponse setBody(String email)
    {
        String code = this.getVerifyCode();

        try
        {

            String body = """
                <h2>Validación de correo</h2>
                <p>Tu código es: <b>%s</b></p>
                """.formatted(code);
            dataAccess.generateVerifyCode(email, code);
            this.send(email, body);
            return new ApiResponse(
                    true,
                    "DEV".equals(DEPLOY) ? code : null
            );
        } catch (Exception e)
        {
            log.error("Error enviando correo => ", e);
            return new ApiResponse(
                    false,
                    "DEV".equals(DEPLOY) ? code : null
            );
        }

    }

    public boolean verifyCode(String code, String email)
    {
        boolean codeIsValid = dataAccess.verifyCode(email, code);
        if(codeIsValid){
            dataAccess.updateCodeAtDatabase(email, code);
        }
        return codeIsValid;
    }

}