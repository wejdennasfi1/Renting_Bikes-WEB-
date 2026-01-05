package rental.api.dto;

public class ReturnRequest {
    private int rentalId;
    private String condition;
    private String note;

    public ReturnRequest() {}

    public int getRentalId() { return rentalId; }
    public void setRentalId(int rentalId) { this.rentalId = rentalId; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
