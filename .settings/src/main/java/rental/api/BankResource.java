package rental.api;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Random;

@Path("/bank")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BankResource {

    public static class BankRequest {
        public String accountId;
        public double amount;
        public String currency;
    }

    public static class BankResponse {
        public boolean accepted;
        public String message;
    }

    @POST
    @Path("/pay")
    public Response pay(BankRequest req) {
        if (req == null || req.accountId == null) {
            return Response.status(400).entity("Invalid request").build();
        }

        BankResponse res = new BankResponse();

        // 🔹 Mock rule: reject if amount > 2000 EUR equivalent
        if (req.amount > 2000) {
            res.accepted = false;
            res.message = "Insufficient funds";
            return Response.status(402).entity(res).build();
        }

        res.accepted = true;
        res.message = "Payment accepted for " + req.amount + " " + req.currency;
        return Response.ok(res).build();
    }
}
