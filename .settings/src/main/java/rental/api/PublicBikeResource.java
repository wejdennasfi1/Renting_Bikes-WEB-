package rental.api;

import rental.data.Db;
import rental.model.Bike;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/public/bikes")
@Produces(MediaType.APPLICATION_JSON)
public class PublicBikeResource {

    @GET
    public List<Bike> listSellableBikes() {
        // EiffelBikeCorp bikes only, rented >= 1, not sold
        return Db.listSellableBikes();
    }
}
