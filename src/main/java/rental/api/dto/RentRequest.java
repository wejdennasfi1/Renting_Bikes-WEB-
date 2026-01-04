package rental.api.dto;

public class RentRequest {
    private int bikeId;
    private int userId;

    public RentRequest() {}

    public int getBikeId() { return bikeId; }
    public void setBikeId(int bikeId) { this.bikeId = bikeId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
}
