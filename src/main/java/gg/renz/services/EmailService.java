package gg.renz.services;

import gg.renz.persistence.DataAccess;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Random;

public class EmailService
{
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    DataAccess dataAccess = new DataAccess();

    private void send(String to, String body)
    {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", "192.168.10.206");
            props.put("mail.smtp.port", "25");

            Session session = Session.getInstance(props);

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("enlinea@sunarp.gob.pe"));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(to)
            );
            message.setSubject("Código de verificación");
            message.setContent(body, "text/html; charset=utf-8");

            Transport.send(message);

        } catch (Exception e)
        {
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