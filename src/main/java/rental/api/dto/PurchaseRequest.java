package rental.api.dto;

public class PurchaseRequest {
    public String userId;
    public String accountId;
    public String currency; // optional (we keep prices in EUR on server)
}
