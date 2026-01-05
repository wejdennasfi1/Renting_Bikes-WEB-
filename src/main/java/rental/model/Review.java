package rental.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import rental.model.*;

public class Review implements Serializable {
    private int id;
    private int bikeId;
    private int userId;
    private String userName;
    private LocalDateTime time;
    private String condition;
    private String text;

    public Review() {}

    public Review(int id, int bikeId, int userId, String userName,
                  LocalDateTime time, String condition, String text) {
        this.id = id;
        this.bikeId = bikeId;
        this.userId = userId;
        this.userName = userName;
        this.time = time;
        this.condition = condition;
        this.text = text;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBikeId() { return bikeId; }
    public void setBikeId(int bikeId) { this.bikeId = bikeId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
