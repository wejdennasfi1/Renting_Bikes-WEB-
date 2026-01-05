package rental.model;

import java.io.Serializable;

public class Bike implements Serializable {
    private int id;
    private String title;
    private String owner;       // "EiffelBikeCorp" or user name
    private double priceEur;
    private boolean available;  // rentable availability
    private int rentedCount;    // eligibility for sale in phase 2
    private boolean sold;       // NEW: if sold, no rent anymore

    public Bike() {}

    public Bike(int id, String title, String owner, double priceEur, boolean available) {
        this.id = id;
        this.title = title;
        this.owner = owner;
        this.priceEur = priceEur;
        this.available = available;
        this.rentedCount = 0;
        this.sold = false;
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

    public boolean isSold() { return sold; }
    public void setSold(boolean sold) { this.sold = sold; }
}
