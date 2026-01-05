package rental.api;

import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import rental.api.dto.RentRequest;
import rental.api.dto.ReturnRequest;
import rental.data.Db;
import rental.model.Rental;

@Path("/rentals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RentalResource {

    @GET
    @Path("/user/{userId}")
    public List<Rental> listForUser(@PathParam("userId") int userId) {
        return Db.listRentalsForUser(userId);
    }

    @POST
    @Path("/rent")
    @Produces(MediaType.TEXT_PLAIN)
    public String rent(RentRequest req) {
        if (req == null) throw new BadRequestException("Missing body");
        return Db.rentBike(req.getBikeId(), req.getUserId());
    }

    @POST
    @Path("/return")
    @Produces(MediaType.TEXT_PLAIN)
    public String giveBack(ReturnRequest req) {
        if (req == null) throw new BadRequestException("Missing body");
        return Db.returnBike(req.getRentalId(), req.getCondition(), req.getNote());
    }
}
