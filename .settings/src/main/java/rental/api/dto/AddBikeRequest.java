package rental.api.dto;

public class AddBikeRequest {
    private int userId;
    private String title;
    private double priceEur;

    public AddBikeRequest() {}

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getPriceEur() { return priceEur; }
    public void setPriceEur(double priceEur) { this.priceEur = priceEur; }
}
