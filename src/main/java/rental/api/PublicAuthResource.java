package rental.api;

import rental.api.dto.LoginRequest;
import rental.data.BankStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/public/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.TEXT_PLAIN)
public class PublicAuthResource {

    @POST
    @Path("/login")
    public Response login(LoginRequest req) {
        if (req == null || req.userId == null || req.userId.trim().isEmpty()) {
            return Response.status(400).entity("Missing userId").build();
        }
        if (req.accountId == null || req.accountId.trim().isEmpty()) {
            return Response.status(400).entity("Missing accountId").build();
        }
        if (req.initialBudgetEur < 0) {
            return Response.status(400).entity("initialBudgetEur must be >= 0").build();
        }

        // store / overwrite the balance for this account
        BankStore.setBalanceEur(req.accountId.trim(), req.initialBudgetEur);

        return Response.ok("OK").build();
    }
}
