package gg.renz.services;

import gg.renz.persistence.DataAccess;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Random;

public class EmailService
{
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final String SMTP_HOST = System.getenv("SMTP_HOST");
    private final String SMTP_PORT = System.getenv("SMTP_PORT");
    private final String SMTP_USER = System.getenv("SMTP_USER");
    private final String SMTP_PASSWORD = System.getenv("SMTP_PASSWORD");
    private final String SMTP_FROM = System.getenv("SMTP_FROM");

    DataAccess dataAccess = new DataAccess();

    private void send(String to, String body)
    {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");

            if ("465".equals(SMTP_PORT)) {
                props.put("mail.smtp.ssl.enable", "true");
            } else {
                props.put("mail.smtp.starttls.enable", "true");
            }

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_FROM));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(to)
            );
            message.setSubject("Código de verificación");
            message.setContent(body, "text/html; charset=utf-8");

            Transport.send(message);
            log.info("Correo enviado exitosamente...");

        } catch (Exception e)
        {
            log.error("Error enviando email", e);
            throw new RuntimeException("Error enviando email", e);
        }
    }

    private String getVerifyCode()
    {
        int m = (int) Math.pow(10, 5);
        return String.valueOf(m + new Random().nextInt(9 * m));
    }

    public boolean setBody(String email)
    {
        try
        {
            String code = this.getVerifyCode();
            String body = """
                <h2>Validación de correo</h2>
                <p>Tu código es: <b>%s</b></p>
                """.formatted(code);
            dataAccess.generateVerifyCode(email, code);
            this.send(email, body);
            return true;
        } catch (Exception e)
        {
            log.error("Error enviando correo => ", e);
            return false;
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