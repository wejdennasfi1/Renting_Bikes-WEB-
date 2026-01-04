package rental.api;

import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import rental.api.dto.AddBikeRequest;
import rental.data.Db;
import rental.model.Bike;

@Path("/bikes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BikeResource {

    @GET
    public List<Bike> list() {
        return Db.listBikes();
    }

    @GET
    @Path("/{id}")
    public Bike get(@PathParam("id") int id) {
        Bike b = Db.getBike(id);
        if (b == null) throw new NotFoundException("Bike not found");
        return b;
    }

    @POST
    public Bike add(AddBikeRequest req) {
        if (req == null) throw new BadRequestException("Missing body");
        if (req.getUserId() <= 0) throw new BadRequestException("userId required");
        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) throw new BadRequestException("title required");

        Bike created = Db.addBike(req.getUserId(), req.getTitle().trim(), req.getPriceEur());
        if (created == null) throw new BadRequestException("Cannot create bike");
        return created;
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String remove(@PathParam("id") int id, @QueryParam("userId") int userId) {
        return Db.removeBike(id, userId);
    }

    @GET
    @Path("/{id}/waiting")
    public List<Integer> waiting(@PathParam("id") int id) {
        return Db.getWaitingList(id);
    }

    @GET
    @Path("/{id}/renter")
    @Produces(MediaType.TEXT_PLAIN)
    public String renter(@PathParam("id") int id) {
        return Db.renterInfo(id);
    }
}
