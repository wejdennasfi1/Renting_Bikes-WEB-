package rental.model;

import java.util.ArrayList;
import java.util.List;

public class Basket {
    private String userId;
    private List<Bike> bikes = new ArrayList<>();

    public Basket(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public List<Bike> getBikes() {
        return bikes;
    }

    public void addBike(Bike b) {
        for (Bike x : bikes) {
            if (x.getId() == b.getId()) return; // avoid duplicates
        }
        bikes.add(b);
    }

    public void clear() {
        bikes.clear();
    }
}
