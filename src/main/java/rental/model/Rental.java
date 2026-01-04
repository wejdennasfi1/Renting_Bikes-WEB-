package rental.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Rental implements Serializable {
    private int id;
    private int bikeId;
    private int userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime; // null while active
    private String returnCondition;
    private String returnNote;

    public Rental() {}

    public Rental(int id, int bikeId, int userId, LocalDateTime startTime) {
        this.id = id;
        this.bikeId = bikeId;
        this.userId = userId;
        this.startTime = startTime;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBikeId() { return bikeId; }
    public void setBikeId(int bikeId) { this.bikeId = bikeId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getReturnCondition() { return returnCondition; }
    public void setReturnCondition(String returnCondition) { this.returnCondition = returnCondition; }

    public String getReturnNote() { return returnNote; }
    public void setReturnNote(String returnNote) { this.returnNote = returnNote; }
}
