package rental.data;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import rental.model.*;

public class Db {

    // Persist under Tomcat folder, example: C:\tomcat9\apache-tomcat-9.0.95\data\final_web_store.json
    private static final String STORE_PATH = buildStorePath();

    private static String buildStorePath() {
        String base = System.getProperty("catalina.base");
        if (base == null || base.trim().isEmpty()) {
            base = System.getProperty("user.home");
        }
        return base + java.io.File.separator + "data" + java.io.File.separator + "final_web_store.json";
    }

    private static final Map<Integer, User> users = new ConcurrentHashMap<>();
    private static final Map<Integer, Bike> bikes = new ConcurrentHashMap<>();
    private static final Map<Integer, Rental> rentals = new ConcurrentHashMap<>();

    private static final Map<Integer, Deque<Integer>> waiting = new ConcurrentHashMap<>();
    private static final Map<Integer, List<String>> notifications = new ConcurrentHashMap<>();

    private static final Map<Integer, Review> reviews = new ConcurrentHashMap<>();
    private static final Map<Integer, List<Integer>> reviewsByBike = new ConcurrentHashMap<>();

    // public baskets: key = clientId
    private static final Map<String, Basket> baskets = new ConcurrentHashMap<>();

    private static final AtomicInteger bikeSeq = new AtomicInteger(1);
    private static final AtomicInteger rentalSeq = new AtomicInteger(1);
    private static final AtomicInteger reviewSeq = new AtomicInteger(1);

    static {
        if (!loadFromDisk()) {
            seed();
            saveToDisk();
        }
    }

    private static void seed() {
        users.clear(); bikes.clear(); rentals.clear();
        waiting.clear(); notifications.clear();
        reviews.clear(); reviewsByBike.clear();
        baskets.clear();

        users.put(1, new User(1, "Sarra", UserType.STUDENT));
        users.put(2, new User(2, "Hamza", UserType.STUDENT));
        users.put(3, new User(3, "Eya", UserType.EMPLOYEE));
        users.put(4, new User(4, "Koussay", UserType.EMPLOYEE));

        Bike b1 = new Bike(1, "Trek FX 2", "EiffelBikeCorp", 450.0, true);
        Bike b2 = new Bike(2, "Decathlon Riverside 500", "EiffelBikeCorp", 300.0, true);
        Bike b3 = new Bike(3, "Giant Escape 3", "Sarra", 380.0, true);
        Bike b4 = new Bike(4, "Btwin Fold 100", "Hamza", 220.0, true);

        bikes.put(1, b1);
        bikes.put(2, b2);
        bikes.put(3, b3);
        bikes.put(4, b4);

        for (int bikeId = 1; bikeId <= 4; bikeId++) {
            waiting.put(bikeId, new ArrayDeque<>());
            reviewsByBike.put(bikeId, new ArrayList<>());
        }
        for (int userId = 1; userId <= 4; userId++) {
            notifications.put(userId, new ArrayList<>());
        }

        // Make them "eligible" for sale by simulating they were rented once (scenario minimal)
        bikes.get(1).setRentedCount(1);
        bikes.get(2).setRentedCount(1);

        bikeSeq.set(5);
        rentalSeq.set(1);
        reviewSeq.set(1);
    }

    // ---------- USERS ----------
    public static List<User> listUsers() {
        List<User> out = new ArrayList<>(users.values());
        out.sort(Comparator.comparingInt(User::getId));
        return out;
    }

    public static User getUser(int id) { return users.get(id); }

    public static List<String> getNotifications(int userId) {
        notifications.putIfAbsent(userId, new ArrayList<>());
        return notifications.get(userId);
    }

    private static void pushNotif(int userId, String msg) {
        notifications.putIfAbsent(userId, new ArrayList<>());
        notifications.get(userId).add(LocalDateTime.now() + " | " + msg);
    }

    // ---------- BIKES ----------
    public static List<Bike> listBikes() {
        List<Bike> out = new ArrayList<>(bikes.values());
        out.sort(Comparator.comparingInt(Bike::getId));
        return out;
    }

    public static Bike getBike(int id) { return bikes.get(id); }

    public static List<Integer> getWaitingList(int bikeId) {
        waiting.putIfAbsent(bikeId, new ArrayDeque<>());
        return new ArrayList<>(waiting.get(bikeId));
    }

    public static synchronized Bike addBike(int userId, String title, double priceEur) {
        User u = users.get(userId);
        if (u == null) return null;
        if (title == null || title.trim().isEmpty()) return null;

        int id = bikeSeq.getAndIncrement();
        Bike b = new Bike(id, title.trim(), u.getName(), priceEur, true);
        b.setSold(false);

        bikes.put(id, b);
        waiting.putIfAbsent(id, new ArrayDeque<>());
        reviewsByBike.putIfAbsent(id, new ArrayList<>());

        saveToDisk();
        return b;
    }

    public static synchronized String removeBike(int bikeId, int userId) {
        Bike b = bikes.get(bikeId);
        User u = users.get(userId);

        if (b == null) return "BIKE_NOT_FOUND";
        if (u == null) return "USER_NOT_FOUND";
        if (!u.getName().equals(b.getOwner())) return "NOT_OWNER";
        if (!b.isAvailable()) return "BIKE_RENTED_CANNOT_REMOVE";
        if (b.isSold()) return "BIKE_SOLD_CANNOT_REMOVE";

        bikes.remove(bikeId);
        waiting.remove(bikeId);

        List<Integer> revIds = reviewsByBike.remove(bikeId);
        if (revIds != null) {
            for (Integer rid : revIds) reviews.remove(rid);
        }

        saveToDisk();
        return "REMOVE_OK";
    }

    public static synchronized String renterInfo(int bikeId) {
        Bike b = bikes.get(bikeId);
        if (b == null) return "BIKE_NOT_FOUND";
        if (b.isSold()) return "SOLD";

        for (Rental r : rentals.values()) {
            if (r.getBikeId() == bikeId && r.getEndTime() == null) {
                User u = users.get(r.getUserId());
                String name = (u != null) ? u.getName() : ("#" + r.getUserId());
                return "RENTED_BY: " + name + " (rentalId=" + r.getId() + ")";
            }
        }
        return "BIKE_AVAILABLE";
    }

    // ---------- RENTALS ----------
    public static List<Rental> listRentalsForUser(int userId) {
        List<Rental> out = new ArrayList<>();
        for (Rental r : rentals.values()) {
            if (r.getUserId() == userId) out.add(r);
        }
        out.sort(Comparator.comparingInt(Rental::getId));
        return out;
    }

    public static synchronized String rentBike(int bikeId, int userId) {
        Bike b = bikes.get(bikeId);
        User u = users.get(userId);

        if (b == null) return "BIKE_NOT_FOUND";
        if (u == null) return "USER_NOT_FOUND";

        if (b.isSold()) return "BIKE_SOLD_NOT_RENTABLE";
        if (u.getName().equals(b.getOwner())) return "CANNOT_RENT_OWN_BIKE";

        if (!b.isAvailable()) {
            waiting.putIfAbsent(bikeId, new ArrayDeque<>());
            Deque<Integer> q = waiting.get(bikeId);
            if (!q.contains(userId)) q.addLast(userId);
            pushNotif(userId, "You are on the waiting list for bike " + b.getTitle());
            saveToDisk();
            return "WAITING_LIST";
        }

        int rid = rentalSeq.getAndIncrement();
        Rental r = new Rental(rid, bikeId, userId, LocalDateTime.now());
        rentals.put(rid, r);

        b.setAvailable(false);
        b.setRentedCount(b.getRentedCount() + 1);

        saveToDisk();
        return "RENT_OK:" + rid;
    }

    // Review is mandatory
    public static synchronized String returnBike(int rentalId, String condition, String note) {
        Rental r = rentals.get(rentalId);
        if (r == null) return "RENTAL_NOT_FOUND";
        if (r.getEndTime() != null) return "ALREADY_RETURNED";

        Bike b = bikes.get(r.getBikeId());
        if (b == null) return "BIKE_NOT_FOUND";

        if (note == null || note.trim().isEmpty()) return "REVIEW_REQUIRED";

        r.setEndTime(LocalDateTime.now());
        r.setReturnCondition(condition);
        r.setReturnNote(note.trim());

        // save review
        User u = users.get(r.getUserId());
        String userName = (u != null) ? u.getName() : ("#" + r.getUserId());

        int revId = reviewSeq.getAndIncrement();
        Review rev = new Review(
                revId, b.getId(), r.getUserId(), userName,
                LocalDateTime.now(),
                (condition == null ? "" : condition),
                note.trim()
        );

        reviews.put(revId, rev);
        reviewsByBike.putIfAbsent(b.getId(), new ArrayList<>());
        reviewsByBike.get(b.getId()).add(revId);

        // make bike available if not sold
        if (!b.isSold()) {
            b.setAvailable(true);
        }

        // auto-rent to first waiting (only if not sold)
        waiting.putIfAbsent(b.getId(), new ArrayDeque<>());
        Deque<Integer> q = waiting.get(b.getId());
        if (!b.isSold() && q != null && !q.isEmpty()) {
            int nextUser = q.removeFirst();
            String result = rentBike(b.getId(), nextUser);
            if (result.startsWith("RENT_OK:")) {
                pushNotif(nextUser, "Bike " + b.getTitle() + " auto-rented for you. RentalId=" + result.split(":")[1]);
            } else {
                pushNotif(nextUser, "Bike " + b.getTitle() + " available but auto-rent failed: " + result);
            }
        }

        saveToDisk();
        return "RETURN_OK";
    }

    public static List<Review> getReviewsForBike(int bikeId) {
        List<Integer> ids = reviewsByBike.get(bikeId);
        if (ids == null) return new ArrayList<>();
        List<Review> out = new ArrayList<>();
        for (Integer id : ids) {
            Review rr = reviews.get(id);
            if (rr != null) out.add(rr);
        }
        out.sort(Comparator.comparingInt(Review::getId));
        return out;
    }

    public static List<Review> getRecentReviewsForBike(int bikeId, int limit) {
        List<Review> all = getReviewsForBike(bikeId);
        all.sort((a, b) -> b.getTime().compareTo(a.getTime())); // newest first
        if (limit <= 0) limit = 5;
        if (all.size() <= limit) return all;
        return new ArrayList<>(all.subList(0, limit));
    }

    // ---------- PHASE 2: PUBLIC SALE ----------
    public static List<Bike> listSellableBikes() {
        List<Bike> out = new ArrayList<>();
        for (Bike b : bikes.values()) {
            String owner = (b.getOwner() == null) ? "" : b.getOwner().trim();
            boolean isCorpBike = owner.equalsIgnoreCase("EiffelBikeCorp");
            if (isCorpBike && b.getRentedCount() > 0 && !b.isSold()) {
                out.add(b);
            }
        }
        out.sort(Comparator.comparingInt(Bike::getId));
        return out;
    }

    public static synchronized String sellBike(int bikeId) {
        Bike b = bikes.get(bikeId);
        if (b == null) return "BIKE_NOT_FOUND";

        String owner = (b.getOwner() == null) ? "" : b.getOwner().trim();
        boolean isCorpBike = owner.equalsIgnoreCase("EiffelBikeCorp");
        if (!isCorpBike) return "NOT_FOR_PUBLIC_SALE";
        if (b.getRentedCount() <= 0) return "NOT_ELIGIBLE_NOT_RENTED";
        if (b.isSold()) return "ALREADY_SOLD";
        if (!b.isAvailable()) return "BIKE_CURRENTLY_RENTED";

        b.setSold(true);
        b.setAvailable(false);
        saveToDisk();
        return "SOLD_OK";
    }

    // ===== PUBLIC BASKET =====
    public static Basket getOrCreateBasket(String userId) {
        baskets.putIfAbsent(userId, new Basket(userId));
        return baskets.get(userId);
    }

    public static synchronized Basket addToBasket(String userId, int bikeId) {
        Basket basket = getOrCreateBasket(userId);

        Bike bike = bikes.get(bikeId);
        if (bike == null) return null;

        String owner = (bike.getOwner() == null) ? "" : bike.getOwner().trim();
        boolean isCorpBike = owner.equalsIgnoreCase("EiffelBikeCorp");
        if (!isCorpBike) return null;
        if (bike.getRentedCount() <= 0) return null;
        if (bike.isSold()) return null;
        if (!bike.isAvailable()) return null;

        basket.addBike(bike);
        return basket;
    }

    public static synchronized String purchaseBasket(String userId, String currency, String accountId) {
        Basket basket = getOrCreateBasket(userId);
        if (basket.getBikes() == null || basket.getBikes().isEmpty()) return "BASKET_EMPTY";

        // total in EUR (stored)
        double totalEur = 0.0;

        for (Bike b : basket.getBikes()) {
            Bike live = bikes.get(b.getId());
            if (live == null) return "BIKE_NOT_FOUND_IN_BASKET";
            if (live.isSold()) return "ALREADY_SOLD_IN_BASKET";
            if (!live.isAvailable()) return "BIKE_NOT_AVAILABLE_IN_BASKET";

            String owner = (live.getOwner() == null) ? "" : live.getOwner().trim();
            if (!owner.equalsIgnoreCase("EiffelBikeCorp")) return "NOT_FOR_PUBLIC_SALE";
            if (live.getRentedCount() <= 0) return "NOT_ELIGIBLE_NOT_RENTED";

            totalEur += live.getPriceEur();
        }

        // convert just for info message
        double rate = FxService.eurTo(currency);
        double totalInCur = totalEur * rate;

        // call bank
        if (accountId == null || accountId.isBlank()) return "ACCOUNT_ID_REQUIRED";
        boolean ok = BankStore.pay(accountId, totalEur);
        if (!ok) {
            return "PAYMENT_REFUSED (insufficient funds). Total=" + totalEur + " EUR";
        }

        // sell bikes
        for (Bike b : basket.getBikes()) {
            String res = sellBike(b.getId());
            if (!"SOLD_OK".equals(res)) {
                return "SELL_FAILED: bikeId=" + b.getId() + " reason=" + res;
            }
        }

        basket.clear();
        saveToDisk();
        return "PURCHASE_OK ✅ Paid " + String.format("%.2f", totalEur) + " EUR (" + String.format("%.2f", totalInCur) + " " + currency + ")";
    }

    // ---------- PERSISTENCE ----------
    private static synchronized void saveToDisk() {
        try {
            java.io.File f = new java.io.File(STORE_PATH);
            java.io.File parent = f.getParentFile();
            if (parent != null) parent.mkdirs();

            JsonStore.writeAll(STORE_PATH, toJson());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean loadFromDisk() {
        try {
            String json = JsonStore.readAll(STORE_PATH);
            if (json == null || json.trim().isEmpty()) return false;
            fromJson(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        sb.append("\"seq\":{")
          .append("\"bike\":").append(bikeSeq.get()).append(",")
          .append("\"rental\":").append(rentalSeq.get()).append(",")
          .append("\"review\":").append(reviewSeq.get())
          .append("},");

        sb.append("\"users\":[");
        boolean first = true;
        for (User u : listUsers()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{")
              .append("\"id\":").append(u.getId()).append(",")
              .append("\"name\":\"").append(JsonStore.esc(u.getName())).append("\",")
              .append("\"type\":\"").append(u.getType().name()).append("\"")
              .append("}");
        }
        sb.append("],");

        sb.append("\"bikes\":[");
        first = true;
        for (Bike b : listBikes()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{")
              .append("\"id\":").append(b.getId()).append(",")
              .append("\"title\":\"").append(JsonStore.esc(b.getTitle())).append("\",")
              .append("\"owner\":\"").append(JsonStore.esc(b.getOwner())).append("\",")
              .append("\"priceEur\":").append(b.getPriceEur()).append(",")
              .append("\"available\":").append(b.isAvailable()).append(",")
              .append("\"rentedCount\":").append(b.getRentedCount()).append(",")
              .append("\"sold\":").append(b.isSold())
              .append("}");
        }
        sb.append("],");

        sb.append("\"rentals\":[");
        first = true;
        List<Rental> rs = new ArrayList<>(rentals.values());
        rs.sort(Comparator.comparingInt(Rental::getId));
        for (Rental r : rs) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{")
              .append("\"id\":").append(r.getId()).append(",")
              .append("\"bikeId\":").append(r.getBikeId()).append(",")
              .append("\"userId\":").append(r.getUserId()).append(",")
              .append("\"startTime\":\"").append(r.getStartTime()).append("\",")
              .append("\"endTime\":").append(r.getEndTime() == null ? "null" : "\"" + r.getEndTime() + "\"").append(",")
              .append("\"returnCondition\":").append(r.getReturnCondition() == null ? "null" : "\"" + JsonStore.esc(r.getReturnCondition()) + "\"").append(",")
              .append("\"returnNote\":").append(r.getReturnNote() == null ? "null" : "\"" + JsonStore.esc(r.getReturnNote()) + "\"")
              .append("}");
        }
        sb.append("],");

        sb.append("\"waiting\":{");
        first = true;
        for (Map.Entry<Integer, Deque<Integer>> e : waiting.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey()).append("\":[");
            boolean f2 = true;
            for (Integer uid : e.getValue()) {
                if (!f2) sb.append(",");
                f2 = false;
                sb.append(uid);
            }
            sb.append("]");
        }
        sb.append("},");

        sb.append("\"notifications\":{");
        first = true;
        for (Map.Entry<Integer, List<String>> e : notifications.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey()).append("\":[");
            boolean f2 = true;
            for (String msg : e.getValue()) {
                if (!f2) sb.append(",");
                f2 = false;
                sb.append("\"").append(JsonStore.esc(msg)).append("\"");
            }
            sb.append("]");
        }
        sb.append("},");

        sb.append("\"reviews\":[");
        first = true;
        List<Review> revs = new ArrayList<>(reviews.values());
        revs.sort(Comparator.comparingInt(Review::getId));
        for (Review r : revs) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{")
              .append("\"id\":").append(r.getId()).append(",")
              .append("\"bikeId\":").append(r.getBikeId()).append(",")
              .append("\"userId\":").append(r.getUserId()).append(",")
              .append("\"userName\":\"").append(JsonStore.esc(r.getUserName())).append("\",")
              .append("\"time\":\"").append(r.getTime()).append("\",")
              .append("\"condition\":\"").append(JsonStore.esc(r.getCondition())).append("\",")
              .append("\"text\":\"").append(JsonStore.esc(r.getText())).append("\"")
              .append("}");
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    private static void fromJson(String json) {
        try {
            users.clear(); bikes.clear(); rentals.clear();
            waiting.clear(); notifications.clear();
            reviews.clear(); reviewsByBike.clear();
            baskets.clear();

            bikeSeq.set(readInt(json, "\"bike\":", 1));
            rentalSeq.set(readInt(json, "\"rental\":", 1));
            reviewSeq.set(readInt(json, "\"review\":", 1));

            for (String obj : extractArrayObjects(json, "\"users\":[")) {
                int id = Integer.parseInt(readField(obj, "id"));
                String name = JsonStore.unesc(stripQuotes(readFieldRaw(obj, "name")));
                String type = stripQuotes(readFieldRaw(obj, "type"));
                users.put(id, new User(id, name, UserType.valueOf(type)));
                notifications.putIfAbsent(id, new ArrayList<>());
            }

            for (String obj : extractArrayObjects(json, "\"bikes\":[")) {
                int id = Integer.parseInt(readField(obj, "id"));
                String title = JsonStore.unesc(stripQuotes(readFieldRaw(obj, "title")));
                String owner = JsonStore.unesc(stripQuotes(readFieldRaw(obj, "owner")));
                double price = Double.parseDouble(readField(obj, "priceEur"));
                boolean available = Boolean.parseBoolean(readField(obj, "available"));
                int rentedCount = Integer.parseInt(readField(obj, "rentedCount"));
                boolean sold = Boolean.parseBoolean(readField(obj, "sold"));

                Bike b = new Bike(id, title, owner, price, available);
                b.setRentedCount(rentedCount);
                b.setSold(sold);
                bikes.put(id, b);

                waiting.putIfAbsent(id, new ArrayDeque<>());
                reviewsByBike.putIfAbsent(id, new ArrayList<>());
            }

            for (String obj : extractArrayObjects(json, "\"rentals\":[")) {
                int id = Integer.parseInt(readField(obj, "id"));
                int bikeId = Integer.parseInt(readField(obj, "bikeId"));
                int userId = Integer.parseInt(readField(obj, "userId"));
                LocalDateTime start = LocalDateTime.parse(stripQuotes(readFieldRaw(obj, "startTime")));

                Rental r = new Rental(id, bikeId, userId, start);

                String endRaw = readFieldRaw(obj, "endTime");
                if (endRaw != null && !"null".equals(endRaw)) r.setEndTime(LocalDateTime.parse(stripQuotes(endRaw)));

                String condRaw = readFieldRaw(obj, "returnCondition");
                if (condRaw != null && !"null".equals(condRaw)) r.setReturnCondition(JsonStore.unesc(stripQuotes(condRaw)));

                String noteRaw = readFieldRaw(obj, "returnNote");
                if (noteRaw != null && !"null".equals(noteRaw)) r.setReturnNote(JsonStore.unesc(stripQuotes(noteRaw)));

                rentals.put(id, r);
            }

            for (String obj : extractArrayObjects(json, "\"reviews\":[")) {
                int id = Integer.parseInt(readField(obj, "id"));
                int bikeId = Integer.parseInt(readField(obj, "bikeId"));
                int userId = Integer.parseInt(readField(obj, "userId"));
                String userName = JsonStore.unesc(stripQuotes(readFieldRaw(obj, "userName")));
                LocalDateTime time = LocalDateTime.parse(stripQuotes(readFieldRaw(obj, "time")));
                String condition = JsonStore.unesc(stripQuotes(readFieldRaw(obj, "condition")));
                String text = JsonStore.unesc(stripQuotes(readFieldRaw(obj, "text")));

                reviews.put(id, new Review(id, bikeId, userId, userName, time, condition, text));
                reviewsByBike.putIfAbsent(bikeId, new ArrayList<>());
                reviewsByBike.get(bikeId).add(id);
            }

        } catch (Exception e) {
            seed();
        }
    }

    private static int readInt(String s, String key, int def) {
        int i = s.indexOf(key);
        if (i < 0) return def;
        i += key.length();
        int j = i;
        while (j < s.length() && (Character.isDigit(s.charAt(j)))) j++;
        try { return Integer.parseInt(s.substring(i, j)); } catch (Exception e) { return def; }
    }

    private static String stripQuotes(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) return s.substring(1, s.length() - 1);
        return s;
    }

    private static List<String> extractArrayObjects(String json, String keyPrefix) {
        List<String> out = new ArrayList<>();
        int start = json.indexOf(keyPrefix);
        if (start < 0) return out;
        start += keyPrefix.length();

        int endArr = findMatching(json, start - 1, '[', ']');
        if (endArr < 0) return out;

        String arr = json.substring(start, endArr).trim();
        int i = 0;
        while (i < arr.length()) {
            int o = arr.indexOf('{', i);
            if (o < 0) break;
            int c = findMatching(arr, o, '{', '}');
            if (c < 0) break;
            out.add(arr.substring(o + 1, c));
            i = c + 1;
        }
        return out;
    }

    private static int findMatching(String s, int start, char open, char close) {
        int depth = 0;
        boolean inStr = false;
        for (int i = start; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '"' && (i == 0 || s.charAt(i - 1) != '\\')) inStr = !inStr;
            if (inStr) continue;
            if (ch == open) depth++;
            if (ch == close) {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static String readField(String obj, String key) {
        String raw = readFieldRaw(obj, key);
        if (raw == null) return null;
        return stripQuotes(raw);
    }

    private static String readFieldRaw(String obj, String key) {
        String k = "\"" + key + "\":";
        int i = obj.indexOf(k);
        if (i < 0) return null;
        i += k.length();

        while (i < obj.length() && Character.isWhitespace(obj.charAt(i))) i++;
        if (i >= obj.length()) return null;

        char c = obj.charAt(i);
        if (c == '"') {
            int j = i + 1;
            while (j < obj.length()) {
                if (obj.charAt(j) == '"' && obj.charAt(j - 1) != '\\') break;
                j++;
            }
            return obj.substring(i, Math.min(j + 1, obj.length()));
        }

        int j = i;
        while (j < obj.length() && obj.charAt(j) != ',') j++;
        return obj.substring(i, j).trim();
    }
}
