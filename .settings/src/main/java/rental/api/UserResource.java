package rental.api;

import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import rental.data.Db;
import rental.model.User;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @GET
    public List<User> list() {
        return Db.listUsers();
    }

    @GET
    @Path("/{id}/notifications")
    public List<String> notifications(@PathParam("id") int id) {
        return Db.getNotifications(id);
    }
}
