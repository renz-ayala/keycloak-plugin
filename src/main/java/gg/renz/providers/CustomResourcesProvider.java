package gg.renz.providers;

import gg.renz.DataTransferObjects.ApiResponse;
import gg.renz.DataTransferObjects.CredentialRequest;
import gg.renz.services.EmailService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomResourcesProvider implements RealmResourceProvider
{
    private static final Logger log = LoggerFactory.getLogger(CustomResourcesProvider.class);
    private final KeycloakSession _session;
    EmailService emailService = new EmailService();

    public CustomResourcesProvider(KeycloakSession session)
    {
        this._session = session;
    }

    @Override
    public Object getResource()
    {
        return this;
    }

    @Override
    public void close()
    {
    }

    @POST
    @Path("generate-code")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ApiResponse generateRandomCode(CredentialRequest request)
    {
        return new ApiResponse(
                emailService.setBody(request.email())
        );
    }

}
