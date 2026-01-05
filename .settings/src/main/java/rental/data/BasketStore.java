package rental.data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class BasketStore {

    // userId -> list of bikeIds
    private static final Map<String, List<Integer>> baskets = new ConcurrentHashMap<>();

    private static List<Integer> basket(String userId) {
        if (userId == null) userId = "";
        return baskets.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
    }

    public static List<Integer> getBikeIds(String userId) {
        return basket(userId);
    }

    public static void clear(String userId) {
        basket(userId).clear();
    }

    public static void addBikeId(String userId, int bikeId) {
        List<Integer> b = basket(userId);
        if (!b.contains(bikeId)) b.add(bikeId);
    }

    public static void removeBikeId(String userId, int bikeId) {
        basket(userId).removeIf(id -> id == bikeId);
    }
}
