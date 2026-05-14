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
import org.keycloak.storage.adapter.AbstractUserAdapter;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.keycloak.storage.user.UserLookupProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomUserStorageProvider implements UserStorageProvider, UserLookupProvider, CredentialInputValidator
{
    private static final Logger log = LoggerFactory.getLogger(CustomUserStorageProvider.class);
    private final KeycloakSession _session;
    private final ComponentModel _model;

    private final String _db_url = System.getenv("ORACLE_DATABASE_URL");
    private final String _db_username = System.getenv("ORACLE_DATABASE_USER");
    private final String _db_password = System.getenv("ORACLE_DATABASE_PASSWORD");

    private final String _USERS_QUERY = "SELECT contrasenia, username, nombres, apellidos, correo, activo FROM user1.sso_users WHERE username = ?";

    public CustomUserStorageProvider(KeycloakSession session, ComponentModel model)
    {
        this._session = session;
        this._model = model;
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

        log.info("!!! INTENTANDO BUSCAR EN ORACLE A: {}", username);

        try
        {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (Exception e)
        {
            log.error("Falla en el driver: ", e);
        }

        log.info("DB_URL: {}", _db_url);
        log.info("DB_USER: {}", _db_username);
        log.info("INTENTANDO CONEXION...");

        try (Connection connection = DriverManager.getConnection(_db_url, _db_username, _db_password))
        {
            log.info("Conexion establecida");

            PreparedStatement statement = connection.prepareStatement(_USERS_QUERY);
            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next())
            {
                log.info("USUARIO ENCONTRADO EN DB: {}", username);
                String names = resultSet.getString("nombres");
                String lastNames = resultSet.getString("apellidos");
                String email = resultSet.getString("correo");
                int active = resultSet.getInt("activo");
                final boolean isActive = active == 1;

                return new AbstractUserAdapterFederatedStorage(_session, realm, _model)
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
                        return names;
                    }

                    @Override
                    public String getLastName()
                    {
                        return lastNames;
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
                        return _session.users().getUserCredentialManager(this);
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
            }else
            {
                log.info("En DB, No existe el usuario : {}", username);
            }

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
        try
        {
            Class.forName("oracle.jdbc.OracleDriver");
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

        var formData = _session.getContext().getHttpRequest().getDecodedFormParameters();
        String code = formData.getFirst("codetoverify");
        String token = formData.getFirst("captchatoken");
        String publicIp = formData.getFirst("public-ip");

        if(!new EmailService().verifyCode(code, user.getUsername())) return false;
        if(!new CaptchaService().verifyCaptcha(token)) return false;

        log.info("codigo y tokenCaptcha valido => VALIDANDO CONECCION CON ORACLE");

        try (Connection connection = DriverManager.getConnection(_db_url, _db_username, _db_password))
        {
            log.info("CONECCION ESTABLECIDA");

            PreparedStatement statement = connection.prepareStatement(_USERS_QUERY);
            statement.setString(1, user.getUsername());

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next())
            {
                log.info("VALIDANDO CONTRASEÑA");

                String dbPassword = resultSet.getString("contrasenia");

                boolean isAuthorized = dbPassword.equals(input.getChallengeResponse());
                log.info("Contraseña válida: {}", isAuthorized);

                if(isAuthorized){
                    String client = _session.getContext().getClient().getClientId();
                    _session.getContext().getAuthenticationSession().setUserSessionNote("docType", "09");
                    _session.getContext().getAuthenticationSession().setUserSessionNote("documentNumber", dbPassword);
                    _session.getContext().getAuthenticationSession().setUserSessionNote("ipAddress", publicIp);
                    _session.getContext().getAuthenticationSession().setUserSessionNote("clientId", client);
                }

                return isAuthorized;
            }

        } catch (SQLException e)
        {
            log.error("Error en la coneccion para la validacion", e);
        }

        log.error("No se pudo validar");
        return false;
    }
}
