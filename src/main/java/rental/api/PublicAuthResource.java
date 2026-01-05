package rental.api;

import rental.api.dto.LoginRequest;
import rental.data.BankStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/public/auth")
@Consumes(MediaType.APPLICATION_JSON)
public class PublicAuthResource {

    // ✅ DTO for returning account/budget
    public static class AccountResponse {
        public String accountId;
        public double balanceEur;

        public AccountResponse() {}

        public AccountResponse(String accountId, double balanceEur) {
            this.accountId = accountId;
            this.balanceEur = balanceEur;
        }
    }

    // ✅ Keep your login (text response)
    @POST
    @Path("/login")
    @Produces(MediaType.TEXT_PLAIN)
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

    // ✅ NEW: Read budget for an account (JSON)
    // GET /api/public/auth/account?accountId=ACC_wejden
    @GET
    @Path("/account")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccount(@QueryParam("accountId") String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return Response.status(400).entity("Missing accountId").build();
        }

        String acc = accountId.trim();

        // If your BankStore.getBalanceEur returns Double, handle null
        // If it returns primitive double, just remove the null check part.
        Double bal = BankStore.getBalanceEur(acc);
        if (bal == null) {
            return Response.status(404).entity("Unknown accountId").build();
        }

        return Response.ok(new AccountResponse(acc, bal)).build();
    }
}
