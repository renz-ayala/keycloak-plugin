package gg.renz.providers;

import gg.renz.services.CaptchaService;
import gg.renz.services.EmailService;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.keycloak.storage.user.UserLookupProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomUserStorageProvider implements UserStorageProvider, UserLookupProvider, CredentialInputValidator
{
    private static final Logger log = LoggerFactory.getLogger(CustomUserStorageProvider.class);

    private final KeycloakSession session;
    private final ComponentModel model;

    private final String DB_URL = System.getenv("ORACLE_DATABASE_URL");
    private final String DB_USERNAME = System.getenv("ORACLE_DATABASE_USER");
    private final String DB_PASSWORD = System.getenv("ORACLE_DATABASE_PASSWORD");


    public CustomUserStorageProvider(KeycloakSession session, ComponentModel model)
    {
        this.session = session;
        this.model = model;
    }

    @Override
    public void close()
    {
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id)
    {
        return null;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username)
    {

        log.info("!!! INTENTANDO BUSCAR A: {}", username);

        try
        {
            //Class.forName("oracle.jdbc.OracleDriver");
            Class.forName("org.postgresql.Driver");
        } catch (Exception e)
        {
            log.error("Falla en el driver: ", e);
        }

        log.info("DB_URL: {}", DB_URL);
        log.info("DB_USER: {}", DB_USERNAME);
        log.info("INTENTANDO CONEXION...");

        String spGetUser = "CALL user1.sp_get_user_by_username(?, ?, ?, ?, ?)";
        try (
                Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                CallableStatement cs = connection.prepareCall(spGetUser)
        )
        {
            log.info("Conexion establecida");
            cs.setString(1, username);

            cs.registerOutParameter(2, Types.VARCHAR); // p_name
            cs.registerOutParameter(3, Types.VARCHAR); // p_last_name
            cs.registerOutParameter(4, Types.VARCHAR); // p_email
            cs.registerOutParameter(5, Types.INTEGER); // p_active

            cs.execute();

            String name = cs.getString(2);
            String lastName = cs.getString(3);
            String email = cs.getString(4);
            int active = cs.getInt(5);
            final boolean isActive = active == 1;

            if (name == null || lastName == null || email == null)
            {
                log.info("No existe el usuario: {}", username);
                return null;
            }

            log.info("USUARIO ENCONTRADO EN DB: {}", username);
            return new AbstractUserAdapterFederatedStorage(session, realm, model)
            {
                @Override
                public String getUsername()
                {
                    return username;
                }

                @Override
                public void setUsername(String username)
                {
                }

                @Override
                public String getFirstName()
                {
                    return name;
                }

                @Override
                public String getLastName()
                {
                    return lastName;
                }

                @Override
                public String getEmail()
                {
                    return email;
                }

                @Override
                public boolean isEmailVerified()
                {
                    return true;
                }

                @Override
                public boolean isEnabled()
                {
                    return isActive;
                }

                @Override
                public SubjectCredentialManager credentialManager()
                {
                    return CustomUserStorageProvider.this.session.users().getUserCredentialManager(this);
                }

                @Override
                public String getFirstAttribute(String name)
                {
                    return super.getFirstAttribute(name);
                }

                @Override
                public Map<String, List<String>> getAttributes()
                {
                    return new HashMap<>(super.getAttributes());
                }

            };

        } catch (SQLException e)
        {
            log.error("ERROR ejecutando el driver manager connection :", e);
        }

        return null;
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email)
    {
        return null;
    }

    @Override
    public boolean supportsCredentialType(String credentialType)
    {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType)
    {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input)
    {
        if (!supportsCredentialType(input.getType()))
        {
            log.error("Error en el tipado de credenciales");
            return false;
        }

        var formData = session.getContext().getHttpRequest().getDecodedFormParameters();
        String code = formData.getFirst("codetoverify");
        String token = formData.getFirst("captchatoken");
        String publicIp = formData.getFirst("public-ip");
        String emailToSendCode = formData.getFirst("emailToSendCode");

        log.info("email Form: {}; email User: {}", emailToSendCode, user.getEmail());
        if (!emailToSendCode.equals(user.getEmail()))
        {
            log.warn("Los correos no son compatibles");
            return false;
        }
        log.info("Correos compatibles");

        var emailService = new EmailService();
        var captchaService = new CaptchaService();

        if(!emailService.verifyCode(code, user.getEmail()))
        {
            log.warn("El codigo no fue verificado correctamente");
            return false;
        }
        log.info("Codigo validado correctamente");

        if(!captchaService.verifyCaptcha(token))
        {
            log.warn("El token captcha es inválido");
            return false;
        }
        log.info("Captcha válido");

        try
        {
            //Class.forName("oracle.jdbc.OracleDriver");
            Class.forName("org.postgresql.Driver");
        }
        catch (Exception e)
        {
            log.error("Falla en el driver", e);
        }

        if (!supportsCredentialType(input.getType()))
        {
            log.error("Error en el tipado de credenciales");
            return false;
        }

        log.info("codigo y tokenCaptcha valido => VALIDANDO CONECCION CON LA BD");
        String spIsValid = "CALL user1.sp_validate_password(?, ?, ?)";
        try (
                Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                CallableStatement cs = connection.prepareCall(spIsValid)
        )
        {
            log.info("CONECCION ESTABLECIDA");

            cs.setString(1, user.getUsername()); //username
            cs.setString(2, input.getChallengeResponse()); //password

            cs.registerOutParameter(3, Types.INTEGER);

            cs.execute();

            int isValidUser = cs.getInt(3);
            boolean isAuthorized = isValidUser == 1;

            log.info("VALIDANDO CONTRASEÑA");
            log.info("Contraseña válida: {}", isAuthorized);

            if(isAuthorized){
                String client = session.getContext().getClient().getClientId();
                session.getContext().getAuthenticationSession().setUserSessionNote("ipAddress", publicIp);
                session.getContext().getAuthenticationSession().setUserSessionNote("clientId", client);
            }

            return isAuthorized;


        } catch (SQLException e)
        {
            log.error("Error en la coneccion para la validacion", e);
            return false;
        }
    }
}
