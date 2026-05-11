package gg.renz;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

public class CustomUserStorageProviderFactory implements UserStorageProviderFactory<CustomUserStorageProvider>
{
    @Override
    public String getId()
    {
        return "oracle-storage-user1";
    }

    @Override
    public CustomUserStorageProvider create(KeycloakSession session, ComponentModel model)
    {
        return new CustomUserStorageProvider(session, model);
    }
}
