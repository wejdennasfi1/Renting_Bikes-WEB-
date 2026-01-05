package rental.api;

import rental.api.dto.AddToBasketRequest;
import rental.api.dto.PurchaseRequest;
import rental.data.BankStore;
import rental.data.BasketStore;
import rental.data.Db;
import rental.model.Bike;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/public/basket")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PublicBasketResource {

    public static class BasketResponse {
        public String userId;
        public List<Bike> bikes;
        public double totalEur;

        public BasketResponse(String userId, List<Bike> bikes, double totalEur) {
            this.userId = userId;
            this.bikes = bikes;
            this.totalEur = totalEur;
        }
    }

    private static List<Bike> resolveBikes(String userId) {
        List<Bike> out = new ArrayList<>();
        for (Integer id : BasketStore.getBikeIds(userId)) {
            Bike b = Db.getBike(id);
            if (b != null) out.add(b);
        }
        return out;
    }

    private static double totalEur(List<Bike> bikes) {
        double t = 0.0;
        for (Bike b : bikes) t += b.getPriceEur();
        return t;
    }

    @GET
    public BasketResponse get(@QueryParam("userId") String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new BadRequestException("Missing userId");
        }
        List<Bike> bikes = resolveBikes(userId);
        return new BasketResponse(userId, bikes, totalEur(bikes));
    }

    @POST
    @Path("/add")
    public BasketResponse add(AddToBasketRequest req) {
        if (req == null || req.userId == null || req.userId.trim().isEmpty()) {
            throw new BadRequestException("Missing userId");
        }

        Bike bike = Db.getBike(req.bikeId);
        if (bike == null) throw new NotFoundException("Bike not found: " + req.bikeId);

        // Gustave rules
        if (bike.isSold()) throw new BadRequestException("Bike already sold");
        if (bike.getRentedCount() <= 0) throw new BadRequestException("Bike not eligible (never rented)");

        BasketStore.addBikeId(req.userId, req.bikeId);

        List<Bike> bikes = resolveBikes(req.userId);
        return new BasketResponse(req.userId, bikes, totalEur(bikes));
    }

    @POST
    @Path("/remove")
    public BasketResponse remove(AddToBasketRequest req) {
        if (req == null || req.userId == null || req.userId.trim().isEmpty()) {
            throw new BadRequestException("Missing userId");
        }

        BasketStore.removeBikeId(req.userId, req.bikeId);

        List<Bike> bikes = resolveBikes(req.userId);
        return new BasketResponse(req.userId, bikes, totalEur(bikes));
    }

    @POST
    @Path("/purchase")
    @Produces(MediaType.TEXT_PLAIN)
    public Response purchase(PurchaseRequest req) {
        if (req == null || req.userId == null || req.userId.trim().isEmpty()) {
            return Response.status(400).entity("Missing userId").build();
        }
        if (req.accountId == null || req.accountId.trim().isEmpty()) {
            return Response.status(400).entity("Missing accountId").build();
        }

        List<Bike> basketBikes = resolveBikes(req.userId);
        if (basketBikes.isEmpty()) {
            return Response.status(400).entity("Basket is empty").build();
        }

        // re-check still purchasable
        for (Bike b : basketBikes) {
            if (b.isSold()) {
                return Response.status(409).entity("Bike already sold: #" + b.getId()).build();
            }
        }

        double total = totalEur(basketBikes);

        boolean paid = BankStore.payEur(req.accountId, total);
        if (!paid) {
            double bal = BankStore.getBalanceEur(req.accountId);
            return Response.status(402)
                    .entity("Payment refused. Balance=" + bal + " EUR, required=" + total + " EUR")
                    .build();
        }

        // ✅ mark sold + unavailable (NO Db.saveBike needed)
        for (Bike b : basketBikes) {
            b.setSold(true);
            b.setAvailable(false);
        }

        BasketStore.clear(req.userId);

        double remaining = BankStore.getBalanceEur(req.accountId);
        return Response.ok("✅ Payment accepted. Total=" + total + " EUR. Remaining=" + remaining + " EUR.").build();
    }
}
