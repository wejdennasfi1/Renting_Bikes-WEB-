package rental.data;

import java.time.LocalDateTime;
import rental.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import rental.model.*;

public class Db {

    private static final Map<Integer, User> users = new ConcurrentHashMap<>();
    private static final Map<Integer, Bike> bikes = new ConcurrentHashMap<>();
    private static final Map<Integer, Rental> rentals = new ConcurrentHashMap<>();

    // waiting list per bike
    private static final Map<Integer, Deque<Integer>> waiting = new ConcurrentHashMap<>();

    // notifications per user
    private static final Map<Integer, List<String>> notifications = new ConcurrentHashMap<>();

    private static final AtomicInteger rentalSeq = new AtomicInteger(1);
    private static final AtomicInteger bikeSeq = new AtomicInteger(5);

    static {
        seed();
    }

    private static void seed() {
        users.put(1, new User(1, "Sarra", UserType.STUDENT));
        users.put(2, new User(2, "Hamza", UserType.STUDENT));
        users.put(3, new User(3, "Eya", UserType.EMPLOYEE));
        users.put(4, new User(4, "Koussay", UserType.EMPLOYEE));

        bikes.put(1, new Bike(1, "Trek FX 2", "EiffelBikeCorp", 450.0, true));
        bikes.put(2, new Bike(2, "Decathlon Riverside 500", "EiffelBikeCorp", 300.0, true));
        bikes.put(3, new Bike(3, "Giant Escape 3", "Sarra", 380.0, true));
        bikes.put(4, new Bike(4, "Btwin Fold 100", "Hamza", 220.0, true));

        waiting.put(1, new ArrayDeque<>());
        waiting.put(2, new ArrayDeque<>());
        waiting.put(3, new ArrayDeque<>());
        waiting.put(4, new ArrayDeque<>());

        notifications.put(1, new ArrayList<>());
        notifications.put(2, new ArrayList<>());
        notifications.put(3, new ArrayList<>());
        notifications.put(4, new ArrayList<>());
    }

    public static List<User> listUsers() {
        return new ArrayList<>(users.values());
    }

    public static List<Bike> listBikes() {
        return new ArrayList<>(bikes.values());
    }

    public static Bike getBike(int id) {
        return bikes.get(id);
    }

    public static User getUser(int id) {
        return users.get(id);
    }

    public static List<Rental> listRentals() {
        return new ArrayList<>(rentals.values());
    }

    public static List<Rental> listRentalsForUser(int userId) {
        List<Rental> out = new ArrayList<>();
        for (Rental r : rentals.values()) {
            if (r.getUserId() == userId) out.add(r);
        }
        return out;
    }

    public static synchronized String rentBike(int bikeId, int userId) {
        Bike b = bikes.get(bikeId);
        if (b == null) return "BIKE_NOT_FOUND";
        if (users.get(userId) == null) return "USER_NOT_FOUND";

        // user cannot rent their own bike
        User u = users.get(userId);
        if (u != null && b.getOwner() != null && b.getOwner().equals(u.getName())) {
            return "CANNOT_RENT_OWN_BIKE";
        }

        if (b.isAvailable()) {
            b.setAvailable(false);
            b.setRentedCount(b.getRentedCount() + 1);

            int rid = rentalSeq.getAndIncrement();
            Rental r = new Rental(rid, bikeId, userId, LocalDateTime.now());
            rentals.put(rid, r);

            return "RENT_OK:" + rid;
        } else {
            waiting.putIfAbsent(bikeId, new ArrayDeque<>());
            Deque<Integer> q = waiting.get(bikeId);

            if (q.contains(userId)) return "ALREADY_WAITING";

            q.addLast(userId);
            return "WAITING_LIST";
        }
    }

    public static synchronized String returnBike(int rentalId, String condition, String note) {
        Rental r = rentals.get(rentalId);
        if (r == null) return "RENTAL_NOT_FOUND";
        if (r.getEndTime() != null) return "ALREADY_RETURNED";

        Bike b = bikes.get(r.getBikeId());
        if (b == null) return "BIKE_NOT_FOUND";

        r.setEndTime(LocalDateTime.now());
        r.setReturnCondition(condition);
        r.setReturnNote(note);

        // bike becomes available
        b.setAvailable(true);

        // check waiting list
        waiting.putIfAbsent(b.getId(), new ArrayDeque<>());
        Deque<Integer> q = waiting.get(b.getId());

        if (q != null && !q.isEmpty()) {
            int nextUser = q.removeFirst();

            // notify the next user
            pushNotif(nextUser, "Bike " + b.getTitle() + " is now available. You were first in waiting list.");

            // auto rent for next user
            String result = rentBike(b.getId(), nextUser);
            if (result.startsWith("RENT_OK:")) {
                pushNotif(nextUser, "Bike " + b.getTitle() + " auto-rented for you. RentalId=" + result.split(":")[1]);
            } else {
                pushNotif(nextUser, "Bike " + b.getTitle() + " became available but auto-rent failed: " + result);
            }
        }

        return "RETURN_OK";
    }

    // NEW: add bike
    public static synchronized Bike addBike(int userId, String title, double priceEur) {
        User u = users.get(userId);
        if (u == null) return null;
        if (title == null || title.trim().isEmpty()) return null;

        int id = bikeSeq.getAndIncrement();
        Bike b = new Bike(id, title.trim(), u.getName(), priceEur, true);

        bikes.put(id, b);
        waiting.putIfAbsent(id, new ArrayDeque<>());
        return b;
    }

    // NEW: remove bike (only owner, only if available)
    public static synchronized String removeBike(int bikeId, int userId) {
        Bike b = bikes.get(bikeId);
        User u = users.get(userId);

        if (b == null) return "BIKE_NOT_FOUND";
        if (u == null) return "USER_NOT_FOUND";

        if (b.getOwner() == null || !b.getOwner().equals(u.getName())) {
            return "NOT_OWNER";
        }

        if (!b.isAvailable()) {
            return "BIKE_RENTED_CANNOT_REMOVE";
        }

        Deque<Integer> q = waiting.get(bikeId);
        if (q != null && !q.isEmpty()) {
            return "WAITING_LIST_NOT_EMPTY";
        }

        bikes.remove(bikeId);
        waiting.remove(bikeId);
        return "REMOVE_OK";
    }

    public static List<Integer> getWaitingList(int bikeId) {
        Deque<Integer> q = waiting.get(bikeId);
        if (q == null) return new ArrayList<>();
        return new ArrayList<>(q);
    }

    public static List<String> getNotifications(int userId) {
        notifications.putIfAbsent(userId, new ArrayList<>());
        return notifications.get(userId);
    }

    private static void pushNotif(int userId, String msg) {
        notifications.putIfAbsent(userId, new ArrayList<>());
        notifications.get(userId).add(LocalDateTime.now() + " | " + msg);
    }
    
    public static synchronized String renterInfo(int bikeId) {
        Bike b = bikes.get(bikeId);
        if (b == null) return "BIKE_NOT_FOUND";

        for (Rental r : rentals.values()) {
            if (r.getBikeId() == bikeId && r.getEndTime() == null) {
                User u = users.get(r.getUserId());
                String name = (u != null) ? u.getName() : ("#" + r.getUserId());
                return "RENTED_BY: " + name + " (rentalId=" + r.getId() + ")";
            }
        }

        return "BIKE_AVAILABLE";
    }

}
