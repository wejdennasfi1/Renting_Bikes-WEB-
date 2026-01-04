package rental.model;

import java.io.Serializable;

public class Bike implements Serializable {
    private int id;
    private String title;          // example: "Trek FX 2"
    private String owner;          // "EiffelBikeCorp" or user name
    private double priceEur;       // sale price later
    private boolean available;
    private int rentedCount;       // used later for Phase 2

    public Bike() {}

    public Bike(int id, String title, String owner, double priceEur, boolean available) {
        this.id = id;
        this.title = title;
        this.owner = owner;
        this.priceEur = priceEur;
        this.available = available;
        this.rentedCount = 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public double getPriceEur() { return priceEur; }
    public void setPriceEur(double priceEur) { this.priceEur = priceEur; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public int getRentedCount() { return rentedCount; }
    public void setRentedCount(int rentedCount) { this.rentedCount = rentedCount; }
}
