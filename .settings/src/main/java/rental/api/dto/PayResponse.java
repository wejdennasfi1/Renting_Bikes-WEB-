package rental.api.dto;

public class PayResponse {
    public boolean ok;
    public String message;
    public double balanceEur;

    public PayResponse() {}

    public PayResponse(boolean ok, String message, double balanceEur) {
        this.ok = ok;
        this.message = message;
        this.balanceEur = balanceEur;
    }
}
