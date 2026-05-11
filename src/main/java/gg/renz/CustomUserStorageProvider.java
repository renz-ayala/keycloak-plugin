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

public class CustomUserStorageProvider
        implements UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator {

    private final KeycloakSession session;
    private final ComponentModel model;

    public CustomUserStorageProvider(
            KeycloakSession session,
            ComponentModel model) {

        this.session = session;
        this.model = model;
    }

    @Override
    public void close() {

    }

    @Override
    public UserModel getUserById(
            RealmModel realm,
            String id) {

        //return getUserByUsername(realm, id);
        return null;
    }

    @Override
    public UserModel getUserByUsername(
            RealmModel realm,
            String username) {

        System.out.println("!!! INTENTANDO BUSCAR EN ORACLE A: " + username);

        try { Class.forName("oracle.jdbc.OracleDriver"); } catch (Exception e) {}

        try (Connection connection = DriverManager.getConnection(
                "jdbc:oracle:thin:@192.168.1.4:1521/XE",
                "SYSTEM",
                "Greg1234.Database")) {

            PreparedStatement statement = connection.prepareStatement(
                    "SELECT username FROM EXTRANET.USUARIOS WHERE username = ?"
            );

            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                System.out.println("!!! USUARIO ENCONTRADO EN DB: " + username);
                final String finalUsername = username;

                AbstractUserAdapter adapter = new AbstractUserAdapter(session, realm, model) {
                    @Override
                    public String getUsername() {
                        return finalUsername;
                    }

                    @Override
                    public SubjectCredentialManager credentialManager() {
                        return session.users().getUserCredentialManager(this);
                    }
                };

                adapter.setSingleAttribute(UserModel.USERNAME, finalUsername);

                return adapter;
            }else {
                System.out.println("!!! LA DB NO DEVOLVIÓ NADA PARA: " + username);
            }

        } catch (SQLException e) {
            System.err.println("!!! ERROR DE SQL EN EL PLUGIN:");
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public UserModel getUserByEmail(
            RealmModel realm,
            String email) {

        return null;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {

        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(
            RealmModel realm,
            UserModel user,
            String credentialType) {

        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(
            RealmModel realm,
            UserModel user,
            CredentialInput input) {

        if (!supportsCredentialType(input.getType())) {
            return false;
        }

        try (Connection connection = DriverManager.getConnection(
                "jdbc:oracle:thin:@192.168.1.4:1521/XE",
                "SYSTEM",
                "Greg1234.Database")) {

            PreparedStatement statement = connection.prepareStatement(
                    "SELECT password FROM EXTRANET.USUARIOS WHERE username = ?"
            );

            statement.setString(1, user.getUsername());

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {

                String dbPassword = resultSet.getString("password");

                return dbPassword.equals(
                        input.getChallengeResponse()
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
