package rental.data;

public class BankClient {

    // In real life: call external bank service via HTTP
    // Here: we simulate it with BankStore
    public static boolean pay(String accountId, double amount, String currency) {
        // amount is provided in EUR by Db.purchaseBasket (currency="EUR")
        return BankStore.payEur(accountId, amount);
    }
}
