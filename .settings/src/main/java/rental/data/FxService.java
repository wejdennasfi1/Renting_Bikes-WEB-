package rental.data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class FxService {

    // fallback offline rates: 1 EUR -> X currency
    private static final Map<String, Double> OFFLINE = new HashMap<>();
    static {
        OFFLINE.put("EUR", 1.0);
        OFFLINE.put("USD", 1.09);
        OFFLINE.put("GBP", 0.85);
        OFFLINE.put("TND", 3.35);
        OFFLINE.put("MAD", 10.85);
    }

    private static Map<String, Double> cache = null;
    private static Instant cacheTime = Instant.EPOCH;
    private static final long CACHE_SECONDS = 60;

    public static double eurTo(String currency) {
        if (currency == null || currency.isBlank()) return 1.0;
        currency = currency.trim().toUpperCase();
        if ("EUR".equals(currency)) return 1.0;

        Map<String, Double> live = getLiveRates();
        Double r = live.get(currency);
        if (r != null && r > 0) return r;

        return OFFLINE.getOrDefault(currency, 1.0);
    }

    private static synchronized Map<String, Double> getLiveRates() {
        try {
            if (cache != null && Instant.now().minusSeconds(CACHE_SECONDS).isBefore(cacheTime)) {
                return cache;
            }

            // Free endpoint (no key needed usually)
            // base=EUR
            URL url = new URL("https://api.exchangerate.host/latest?base=EUR");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);
            con.setRequestMethod("GET");

            int code = con.getResponseCode();
            if (code != 200) throw new RuntimeException("HTTP " + code);

            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            // VERY small JSON parsing (we only need "rates":{"USD":...})
            String json = sb.toString();
            Map<String, Double> out = new HashMap<>();
            out.put("EUR", 1.0);

            int ratesIdx = json.indexOf("\"rates\"");
            if (ratesIdx < 0) throw new RuntimeException("no rates");
            int objStart = json.indexOf("{", ratesIdx);
            int objEnd = json.indexOf("}", objStart);
            if (objStart < 0 || objEnd < 0) throw new RuntimeException("bad json");

            String body = json.substring(objStart + 1, objEnd);
            String[] parts = body.split(",");
            for (String p : parts) {
                String[] kv = p.split(":");
                if (kv.length != 2) continue;
                String k = kv[0].trim().replace("\"", "");
                String v = kv[1].trim();
                try {
                    out.put(k, Double.parseDouble(v));
                } catch (Exception ignore) {}
            }

            cache = out;
            cacheTime = Instant.now();
            return out;

        } catch (Exception e) {
            // fallback offline only
            return OFFLINE;
        }
    }
}
