package rental.model;

import java.io.Serializable;

public class User implements Serializable {
    private int id;
    private String name;
    private UserType type;

    public User() {}

    public User(int id, String name, UserType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UserType getType() { return type; }
    public void setType(UserType type) { this.type = type; }
}
