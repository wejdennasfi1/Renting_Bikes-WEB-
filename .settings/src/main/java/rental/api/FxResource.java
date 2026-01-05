package rental.api;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

@Path("/fx")
@Produces(MediaType.APPLICATION_JSON)
public class FxResource {

    public static class FxResponse {
        public String from;
        public String to;
        public double rate;
        public String time;

        public FxResponse(String from, String to, double rate) {
            this.from = from;
            this.to = to;
            this.rate = rate;
            this.time = Instant.now().toString();
        }
    }

    @GET
    public Response fx(@QueryParam("from") @DefaultValue("EUR") String from,
                       @QueryParam("to") String to) {

        if (to == null || to.trim().isEmpty()) {
            return Response.status(400).entity("{\"error\":\"Missing 'to' currency\"}").build();
        }

        from = from.trim().toUpperCase();
        to = to.trim().toUpperCase();

        if (from.equals(to)) {
            return Response.ok(new FxResponse(from, to, 1.0)).build();
        }

        String url = "https://api.exchangerate.host/convert?from=" + from + "&to=" + to;

        try {
            HttpClient client = HttpClient.newBuilder().build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() != 200) {
                return Response.status(502)
                        .entity("{\"error\":\"FX provider HTTP " + res.statusCode() + "\"}")
                        .build();
            }

            String body = res.body();

            String needle = "\"result\":";
            int i = body.indexOf(needle);
            if (i < 0) return Response.status(502).entity("{\"error\":\"FX rate not found\"}").build();

            i += needle.length();
            int j = i;
            while (j < body.length() && "0123456789.-".indexOf(body.charAt(j)) >= 0) j++;

            double rate = Double.parseDouble(body.substring(i, j));

            return Response.ok(new FxResponse(from, to, rate)).build();

        } catch (Exception e) {
            return Response.status(502)
                    .entity("{\"error\":\"FX unavailable: " + escape(e.getMessage()) + "\"}")
                    .build();
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "'").replace("\n", " ").replace("\r", " ");
    }
}
