package gg.renz;

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
import org.keycloak.storage.user.UserLookupProvider;

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
    private final KeycloakSession _session;
    private final ComponentModel _model;

    String _db_url = "jdbc:oracle:thin:@172.18.1.73:1521/DESABODEGA";
    String _db_username = "user1";
    String _db_password = "user1";

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
        //return getUserByUsername(realm, id);
        return null;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username)
    {

        System.out.println("!!! INTENTANDO BUSCAR EN ORACLE A: " + username);

        try
        {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (Exception e)
        {
            System.out.println("FALLA EN EL CLASS FORNAME");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        System.out.println("DB_URL: " + _db_url);
        System.out.println("DB_USER: " + _db_username);
        System.out.println("INTENTANDO CONEXION...");

        try (Connection connection = DriverManager.getConnection(_db_url, _db_username, _db_password))
        {
            System.out.println("LLEGO AL QUERY");

            PreparedStatement statement = connection.prepareStatement(
                    "SELECT username, nombres, apellidos, correo, activo FROM user1.sso_users WHERE username = ?"
            );

            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next())
            {
                String nombres = resultSet.getString("nombres");
                String apellidos = resultSet.getString("apellidos");
                String correo = resultSet.getString("correo");
                int activo = resultSet.getInt("activo");

                System.out.println("!!! USUARIO ENCONTRADO EN DB: " +  username);

                final String finalUsername = username;
                final String finalNombres = nombres;
                final String finalApellidos = apellidos;
                final String finalCorreo = correo;
                final boolean finalActivo = activo == 1;

                AbstractUserAdapter adapter = new AbstractUserAdapter(_session, realm, _model)
                {
                    @Override
                    public String getUsername()
                    {
                        return finalUsername;
                    }

                    @Override
                    public String getFirstName()
                    {
                        return finalNombres;
                    }

                    @Override
                    public String getLastName()
                    {
                        return finalApellidos;
                    }

                    @Override
                    public String getEmail()
                    {
                        return finalCorreo;
                    }

                    @Override
                    public boolean isEmailVerified()
                    {
                        return true;
                    }

                    @Override
                    public boolean isEnabled()
                    {
                        return finalActivo;
                    }

                    @Override
                    public SubjectCredentialManager credentialManager()
                    {
                        return _session.users().getUserCredentialManager(this);
                    }

                    @Override
                    public String getFirstAttribute(String name)
                    {
                        return switch (name)
                        {
                            case UserModel.EMAIL -> finalCorreo;
                            case UserModel.FIRST_NAME -> finalNombres;
                            case UserModel.LAST_NAME -> finalApellidos;
                            default -> null;
                        };
                    }

                    @Override
                    public Map<String, List<String>> getAttributes()
                    {
                        Map<String, List<String>> attributes = new HashMap<>();

                        attributes.put(UserModel.USERNAME, List.of(finalUsername));
                        attributes.put(UserModel.EMAIL, List.of(finalCorreo));
                        attributes.put(UserModel.FIRST_NAME, List.of(finalNombres));
                        attributes.put(UserModel.LAST_NAME, List.of(finalApellidos));

                        return attributes;
                    }

                };

                //adapter.setSingleAttribute(UserModel.USERNAME, finalUsername);

                return adapter;
            }else
            {
                System.out.println("!!! LA DB NO DEVOLVIÓ NADA PARA: " + username);
            }

        } catch (SQLException e)
        {
            System.err.println("!!! ERROR DE SQL EN EL PLUGIN:");
            e.printStackTrace();
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
            System.out.println("FALLA EN CLASS FORNAME EN ISVALID");
            e.printStackTrace();
        }

        if (!supportsCredentialType(input.getType()))
        {
            System.out.println("FALLO EL SUPPORT");
            return false;
        }

        System.out.println("DB_URL: " + _db_url);
        System.out.println("DB_USER: " + _db_username);
        System.out.println("INTENTANDO CONEXION...");

        try (Connection connection = DriverManager.getConnection(_db_url, _db_username, _db_password))
        {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT contrasenia FROM user1.sso_users WHERE username = ?"
            );

            statement.setString(1, user.getUsername());

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next())
            {
                String dbPassword = resultSet.getString("contrasenia");

                return dbPassword.equals(
                        input.getChallengeResponse()
                );
            }

        } catch (SQLException e)
        {
            System.out.println("FALLO LA CONECCION");
            e.printStackTrace();
        }

        return false;
    }
}
